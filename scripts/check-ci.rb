require 'yaml'
require 'open3'

ROOT = File.expand_path('..', __dir__)
tests = YAML.load_file(File.join(ROOT, '.github/workflows/e2e.yml'))
deploy = YAML.load_file(File.join(ROOT, '.github/workflows/deploy.yml'))
# Psych versions using YAML 1.1 parse the key "on" as true.
events = tests['on'] || tests[true]

def check(condition, message)
  raise message unless condition
end

%w[backend flights-service frontend].each do |mod|
  workflow = YAML.load_file(File.join(ROOT, ".github/workflows/#{mod}.yml"))
  triggers = workflow['on'] || workflow[true]
  call = triggers.fetch('workflow_call')
  check(call.dig('inputs', 'checks-only', 'type') == 'boolean', "#{mod} must support checks-only calls")
  check(call.dig('inputs', 'checks-only', 'default') == true, "#{mod} calls must skip publishing by default")
  check(workflow.dig('jobs', 'publish', 'needs') == 'build', "#{mod} publishing must require build")
  condition = workflow.dig('jobs', 'publish', 'if')
  check(condition == "github.ref == 'refs/heads/main' && github.event_name == 'push' && inputs.checks-only != true", "#{mod} must guard publishing")
  # Evaluate the actual guard for direct events and reusable calls from main.
  [['main', 'push', false, true], ['main', 'push', true, false],
   ['dev', 'push', false, false], ['main', 'pull_request', false, false],
   ['main', 'workflow_dispatch', true, false]].each do |branch, event, checks_only, expected|
    expression = condition.gsub('github.ref', "'refs/heads/#{branch}'")
                          .gsub('github.event_name', event.inspect)
                          .gsub('inputs.checks-only', checks_only.to_s)
    check(eval(expression) == expected, "#{mod} publishing guard failed for #{branch}/#{event}/#{checks_only}")
  end
end

check(events.key?('workflow_call'), 'Test workflow must be reusable')
%w[pull_request push].each do |event|
  check(events.fetch(event).fetch('branches').sort == %w[dev main], "Missing #{event} triggers")
end
%w[acceptance e2e].each do |suite|
  steps = tests.fetch('jobs').fetch(suite).fetch('steps')
  check(steps.any? { |step| step['if'] == 'always()' && step['run'].to_s.include?('down') }, "Missing #{suite} cleanup")
  check(steps.none? { |step| step['continue-on-error'] }, "#{suite} must propagate failures")
  check(steps.none? { |step| step['uses'].to_s.start_with?('actions/upload-artifact@') }, 'CI must use logs only')
end

jobs = deploy.fetch('jobs')
check(jobs.fetch('tests')['uses'] == './.github/workflows/e2e.yml', 'Deploy must reuse test workflow')
check(jobs.fetch('tests')['if'] == "inputs.action == 'deploy'", 'Stop must skip tests')
check(jobs.fetch('stop')['needs'].nil?, 'Stop must remain independent')
%w[backend flights-service frontend].each do |mod|
  job = jobs.fetch(mod)
  check(job['uses'] == "./.github/workflows/#{mod}.yml", "Deploy must reuse #{mod} checks")
  check(job['if'] == "inputs.action == 'deploy'", "Stop must skip #{mod}")
  check(job.dig('with', 'checks-only') == true, "#{mod} must not publish during validation")
  check(!job['continue-on-error'], "#{mod} must propagate failures")
  workflow = YAML.load_file(File.join(ROOT, ".github/workflows/#{mod}.yml"))
  check(workflow.dig('jobs', 'build', 'permissions') == { 'contents' => 'read' }, "#{mod} checks must use a read-only token")
end
check(Array(jobs.fetch('publish')['needs']).sort == %w[backend flights-service frontend tests], 'Publishing must require all passing checks')
check(jobs.fetch('publish')['if'].nil?, 'Publishing must not bypass failed checks')
check(Array(jobs.fetch('deploy')['needs']).sort == %w[publish tests], 'Deploy must require tests and publishing')
check(jobs.fetch('deploy')['if'] == "inputs.action == 'deploy'", 'Deploy must not bypass failure gating')
publish = jobs.fetch('publish')
check(publish.dig('strategy', 'matrix', 'service').sort == %w[backend flights-service frontend], 'Publish all three images')
build = publish['steps'].find { |step| step['uses'].to_s.start_with?('docker/build-push-action@') }
check(build.dig('with', 'tags').include?('${{ github.sha }}'), 'Published images must identify the tested SHA')
ssh = jobs.fetch('deploy')['steps'].find { |step| step['uses'].to_s.start_with?('appleboy/ssh-action@') }
script = ssh.dig('with', 'script')
%w[backend flights-service frontend].each do |service|
  check(script.include?("ghcr.io/acostaf12/ctv-#{service}:${{ github.sha }}"), "#{service} must deploy tested SHA")
end
check(script.include?('set -eu'), 'Remote deploy must stop if image pull fails')

# Exercise the real shell control flow without contacting Docker or Azure.
local_script = script.sub('cd /opt/ctv', ':').gsub('${{ github.sha }}', '0' * 40)
output, _, status = Open3.capture3('sh', stdin_data: "docker() { echo called-docker; return 17; }\n#{local_script}")
check(status.exitstatus == 17 && output.lines.size == 1, 'Failed pull must prevent up')

puts 'CI contract checks passed'
