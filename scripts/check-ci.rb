require 'yaml'
require 'open3'
require 'tmpdir'
require 'fileutils'

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

%w[backend flights-service].each do |mod|
  workflow = YAML.load_file(File.join(ROOT, ".github/workflows/#{mod}.yml"))
  steps = workflow.fetch('jobs').fetch('build').fetch('steps')
  sonar = steps.find { |step| step['name'] == 'SonarCloud analysis and Quality Gate' }
  check(!sonar.nil?, "#{mod} must enforce the Sonar Quality Gate")
  check(sonar['if'] == "github.event_name != 'pull_request' || github.event.pull_request.head.repo.full_name == github.repository", "#{mod} must only skip Sonar for fork PRs")
  check(!sonar['continue-on-error'], "#{mod} must propagate Sonar failures")
  # Run the real branch logic, replacing only Gradle to avoid contacting Sonar.
  stub = "gradle_stub() { printf '%s\\n' \"$@\"; return \"$SONAR_EXIT_CODE\"; }\n"
  script = stub + sonar.fetch('run').gsub('./gradlew', 'gradle_stub')
  {
    'refs/heads/main' => true,
    'refs/heads/dev' => false,
    'refs/heads/feature/ci-test' => false,
    'refs/pull/22/merge' => false,
  }.each do |ref, wait|
    [['', 0], ['local-test-token', 0], ['local-test-token', 23]].each do |token, exit_code|
      env = { 'SONAR_TOKEN' => token, 'GITHUB_REF' => ref, 'SONAR_EXIT_CODE' => exit_code.to_s }
      output, _, status = Open3.capture3(env, 'bash', '-e', '-o', 'pipefail', stdin_data: script)
      expected_exit = token.empty? ? 1 : exit_code
      check(status.exitstatus == expected_exit, "#{mod} must reject missing tokens and propagate analysis failures on #{ref}")
      if token.empty?
        check(output.include?('SONAR_TOKEN'), "#{mod} must explain missing configuration")
        check(!output.include?('-Dsonar.'), "#{mod} must reject missing tokens before running Sonar")
      else
        expected_args = ['sonar', "-Dsonar.qualitygate.wait=#{wait}", '-Dsonar.qualitygate.timeout=300']
        check(output.lines.map(&:chomp) == expected_args, "#{mod} must analyze #{ref} with Quality Gate wait=#{wait}")
        check(!output.include?(token), "#{mod} must not expose tokens")
      end
    end
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

load = YAML.load_file(File.join(ROOT, '.github/workflows/load-tests.yml'))
load_events = load['on'] || load[true]
check(load_events.keys == ['workflow_dispatch'], 'Load tests must only run manually')
check(load['permissions'] == { 'contents' => 'read' }, 'Load tests must use a read-only token')
scenarios = %w[catalog-load login-load last-seat-stress catalog-spike]
check(load_events.dig('workflow_dispatch', 'inputs', 'scenario', 'options') == ['all'] + scenarios, 'Load tests must offer every scenario')
check(load_events.dig('workflow_dispatch', 'inputs', 'target', 'options') == %w[runner vm], 'Load tests must target the runner or the VM')
load_steps = load.fetch('jobs').fetch('k6').fetch('steps')
check(load_steps.any? { |step| step['if'].to_s.start_with?('always()') && step['run'].to_s.include?('down') }, 'Missing load test cleanup')
check(load_steps.none? { |step| step['continue-on-error'] }, 'Load tests must propagate failures')
check(load_steps.none? { |step| step['uses'].to_s.start_with?('actions/upload-artifact@') }, 'CI must use logs only')
scenarios.each do |scenario|
  step = load_steps.find { |s| s['name'] == scenario }
  check(!step.nil? && File.exist?(File.join(ROOT, "load-tests/#{scenario}.js")), "Missing load test #{scenario}")
  # Every scenario runs even if a previous one failed, but never against an unhealthy target.
  check(step['if'].include?("!cancelled() && steps.ready.outcome == 'success'"), "#{scenario} must run after earlier failures")
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
check(jobs.fetch('workflow-validation')['uses'] == './.github/workflows/workflow-validation.yml', 'Deploy must reuse workflow validation')
check(jobs.fetch('workflow-validation')['if'] == "inputs.action == 'deploy'", 'Stop must skip workflow validation')
check(Array(jobs.fetch('publish')['needs']).sort == %w[backend flights-service frontend preflight tests workflow-validation], 'Publishing must require all passing checks')
check(jobs.fetch('preflight')['if'] == "inputs.action == 'deploy'", 'Stop must skip the deploy preflight')
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

# The JWT secret travels as an SSH environment variable, never inside the script text.
check(!script.include?('secrets.'), 'Remote deploy script must not embed secrets')
check(ssh.dig('env', 'JWT_SECRET') == '${{ secrets.JWT_SECRET }}' && ssh.dig('with', 'envs').to_s.split(',').include?('JWT_SECRET'), 'Deploy must pass JWT_SECRET to the VM')
check(ssh.dig('env', 'VM_HOST') == '${{ secrets.VM_HOST }}' && ssh.dig('with', 'envs').to_s.split(',').include?('VM_HOST'), 'Deploy must pass VM_HOST to the VM for CORS')
health = jobs.fetch('deploy')['steps'].find { |step| step['name'] == 'Health check' }.to_h['run'].to_s
check(health.include?('Origin: $origin') && health.include?('access-control-allow-origin: $origin') && health.include?('exit 1'), 'Deploy must fail when the API rejects the VM frontend origin')
preflight = jobs.fetch('preflight').fetch('steps').map { |step| step['run'].to_s }.join("\n")
check(preflight.include?('JWT_SECRET') && preflight.include?('exit 1'), 'Deploy must fail early without JWT_SECRET')

# Exercise the real shell control flow with a fake release, without contacting Docker, GitHub or Azure.
sha = '0' * 40
Dir.mktmpdir do |dir|
  release = File.join(dir, 'src', "unq-pdes-2026c2-ctv-#{sha}")
  FileUtils.mkdir_p(File.join(release, 'monitoring/grafana'))
  File.write(File.join(release, 'docker-compose.yml'), "new-compose\n")
  File.write(File.join(release, 'docker-compose.prod.yml'), "new-prod\n")
  File.write(File.join(release, 'monitoring/prometheus.yml'), "new-prometheus\n")
  File.write(File.join(release, 'monitoring/grafana/x.yml'), "new-grafana\n")
  tarball = File.join(dir, 'release.tar.gz')
  _, err, status = Open3.capture3('tar', '-czf', tarball, '-C', File.join(dir, 'src'), File.basename(release))
  check(status.success?, "Could not build the fake release: #{err}")

  run_deploy = lambda do |name, env|
    vm = File.join(dir, "vm-#{name}")
    FileUtils.mkdir_p(File.join(vm, 'monitoring'))
    File.write(File.join(vm, 'docker-compose.yml'), "old-compose\n")
    File.write(File.join(vm, 'monitoring/prometheus.yml'), "old-prometheus\n")
    # The VM had private config directories that Grafana (uid 472) could not read.
    FileUtils.chmod(0o700, File.join(vm, 'monitoring'))
    File.write(File.join(vm, '.env'), "CORS_ALLOWED_ORIGIN_PATTERNS=http://vm:*\nSEED_ENABLED=false\nJWT_SECRET='old'\n")
    log = File.join(dir, "calls-#{name}.log")
    stubs = <<~SH
      curl() { echo "curl $*" >> "#{log}"; cat "#{tarball}"; }
      sudo() { echo "unexpected sudo" >> "#{log}"; return 99; }
      docker() { echo "docker $*" >> "#{log}"; cat > /dev/null; return 17; }
    SH
    local_script = script.sub('cd /opt/ctv', "cd '#{vm}'").gsub('${{ github.sha }}', sha)
    _, _, result = Open3.capture3(env, 'sh', stdin_data: stubs + local_script)
    [vm, File.exist?(log) ? File.read(log) : '', result]
  end

  vm, log, status = run_deploy.call('missing', { 'JWT_SECRET' => '', 'VM_HOST' => '203.0.113.7' })
  check(!status.success? && log.empty?, 'Deploy must stop before touching the VM without JWT_SECRET')
  check(File.read(File.join(vm, 'docker-compose.yml')) == "old-compose\n", 'Deploy without JWT_SECRET must not sync files')

  jwt = 'local-test-secret-with-at-least-32-bytes'
  [['nohost', ''], ['badhost', "203.0.113.7\nJWT_SECRET=x"]].each do |name, host|
    vm, log, status = run_deploy.call(name, { 'JWT_SECRET' => jwt, 'VM_HOST' => host })
    check(!status.success? && log.empty?, "Deploy must stop before touching the VM with VM_HOST=#{host.inspect}")
    check(File.read(File.join(vm, '.env')).include?("JWT_SECRET='old'"), 'Deploy with an invalid VM_HOST must not rewrite .env')
  end

  vm, log, status = run_deploy.call('release', { 'JWT_SECRET' => jwt, 'VM_HOST' => '203.0.113.7' })
  check(status.exitstatus == 17, 'Failed pull must fail the deploy')
  check(log.lines.grep(/^docker /).size == 1 && log.include?('pull'), 'Failed pull must prevent up')
  check(log.include?("codeload.github.com/AcostaF12/unq-pdes-2026c2-ctv/tar.gz/#{sha}"), 'Deploy must download the deployed commit')
  check(!log.include?('unexpected sudo'), 'Deploy must not escalate when it owns /opt/ctv')
  check(File.read(File.join(vm, 'docker-compose.yml')) == "new-compose\n" &&
        File.read(File.join(vm, 'docker-compose.prod.yml')) == "new-prod\n", 'Deploy must sync the Compose files')
  check(File.read(File.join(vm, 'monitoring/prometheus.yml')) == "new-prometheus\n" &&
        File.read(File.join(vm, 'monitoring/grafana/x.yml')) == "new-grafana\n", 'Deploy must sync the monitoring config')
  env_file = File.read(File.join(vm, '.env'))
  check(env_file.include?("SEED_ENABLED=false\n") && env_file.scan(/^JWT_SECRET=/).size == 1 &&
        env_file.include?("JWT_SECRET='#{jwt}'"), 'Deploy must store JWT_SECRET in .env keeping other settings')
  check(env_file.scan(/^CORS_ALLOWED_ORIGIN_PATTERNS=.*$/) == ['CORS_ALLOWED_ORIGIN_PATTERNS=http://203.0.113.7:8090'],
        'Deploy must allow the VM frontend origin in .env')
  unless Gem.win_platform?
    check(File.stat(File.join(vm, '.env')).mode & 0o777 == 0o600, '.env must only be readable by the deploy user')
    readable = Dir.glob(File.join(vm, 'monitoring', '**', '*')).push(File.join(vm, 'monitoring')).all? do |path|
      mode = File.stat(path).mode
      mode & 0o004 != 0 && (!File.directory?(path) || mode & 0o001 != 0)
    end
    check(readable, 'Monitoring config must be readable by the non-root monitoring containers')
  end
end

# Image hardening: non-root processes, cached dependencies and pinned third-party images.
%w[backend flights-service].each do |mod|
  dockerfile = File.read(File.join(ROOT, mod, 'Dockerfile'))
  check(dockerfile.match?(/^USER (?!root\b|0\b)\S+/), "#{mod} image must run as a non-root user")
  check(dockerfile.index('gradlew resolveDependencies').to_i.between?(1, dockerfile.index('COPY src').to_i), "#{mod} must download dependency jars before copying sources")
  check(dockerfile.match?(/^COPY .*\bgradle\.properties\b.*\n(?:.*\n)*RUN \.\/gradlew resolveDependencies/), "#{mod} must copy gradle.properties before downloading dependencies")
  check(dockerfile.match?(/^RUN --network=none \.\/gradlew bootJar .*--offline/), "#{mod} must compile offline from the dependency layer")
  check(File.read(File.join(ROOT, mod, 'build.gradle.kts')).include?('tasks.register("resolveDependencies")'), "#{mod} must define resolveDependencies")
end
check(File.read(File.join(ROOT, 'frontend/Dockerfile')).include?('nginx-unprivileged'), 'frontend image must run nginx as non-root')
%w[docker-compose.yml acceptance-tests/docker-compose.yml backend/Dockerfile flights-service/Dockerfile frontend/Dockerfile].each do |file|
  File.read(File.join(ROOT, file)).scan(/^\s*(?:image:|FROM)\s+(\S+)/).flatten.each do |image|
    check(image.match?(%r{:[^/]+\z}) && !image.end_with?(':latest'), "#{file} must pin #{image}")
  end
end
prod = File.read(File.join(ROOT, 'docker-compose.prod.yml'))
%w[backend-postgres flights-postgres elasticsearch logstash prometheus].each do |service|
  check(prod.match?(/^  #{Regexp.escape(service)}:\n    ports: !reset \[\]$/), "Production must not publish #{service} ports")
end
check(prod.include?('JWT_SECRET: ${JWT_SECRET:?'), 'Production must require JWT_SECRET')

# Gradle jobs cache ~/.gradle between runs and retry only transient repository errors (HTTP 429).
%w[backend flights-service].each do |mod|
  steps = YAML.load_file(File.join(ROOT, ".github/workflows/#{mod}.yml")).dig('jobs', 'build', 'steps')
  setup = steps.find { |step| step['uses'].to_s.start_with?('gradle/actions/setup-gradle@') }
  check(!setup.nil? && setup['uses'].match?(/@v\d+\.\d+\.\d+\z/), "#{mod} must cache Gradle with a pinned setup-gradle")
  check(setup.dig('with', 'cache-disabled') != true, "#{mod} must keep the Gradle cache enabled")
  check(steps.none? { |step| step['uses'].to_s.start_with?('actions/setup-java@') && step.dig('with', 'cache') }, "#{mod} must not duplicate the Gradle cache")
  build = steps.find { |step| step['name'] == 'Build and test' }
  check(build && build['run'] == '../scripts/ci-gradle.sh build', "#{mod} must build through the retrying Gradle wrapper")
  props = File.read(File.join(ROOT, mod, 'gradle.properties'))
  check(props.include?('systemProp.org.gradle.internal.repository.max.tentatives='), "#{mod} must extend Gradle's repository retries")
end
# Exercise the retry loop with a fake wrapper: transient errors retry, real failures do not.
Dir.mktmpdir do |dir|
  run_gradle = lambda do |outputs|
    File.write(File.join(dir, 'outputs'), outputs.join("\n") + "\n")
    File.write(File.join(dir, 'calls'), '')
    File.write(File.join(dir, 'gradlew'), <<~SH)
      #!/usr/bin/env bash
      echo "$*" >> calls
      line=$(sed -n "$(wc -l < calls)p" outputs)
      echo "$line"
      [ "$line" = ok ]
    SH
    FileUtils.chmod(0o755, File.join(dir, 'gradlew'))
    env = { 'GRADLE_ATTEMPTS' => '3', 'GRADLE_RETRY_DELAY' => '0' }
    _, _, status = Open3.capture3(env, 'bash', File.join(ROOT, 'scripts/ci-gradle.sh'), 'build', chdir: dir)
    [status.exitstatus, File.read(File.join(dir, 'calls')).lines.size]
  end
  rate_limited = 'Could not GET x. Received status code 429 from server: Too Many Requests'
  check(run_gradle.call([rate_limited, 'ok']) == [0, 2], 'Gradle builds must retry HTTP 429')
  check(run_gradle.call([rate_limited] * 3) == [1, 3], 'Gradle retries must be bounded')
  check(run_gradle.call(["e: Unresolved reference 'foo'", 'ok']) == [1, 1], 'Compilation failures must not be retried')
end

# CI image builds share cached layers and retry transient registry errors.
%w[acceptance e2e].each do |suite|
  steps = tests.fetch('jobs').fetch(suite).fetch('steps')
  check(steps.any? { |step| step['run'].to_s.include?('scripts/ci-build-images.sh') }, "#{suite} must build images with cache and retry")
  check(steps.any? { |step| step['uses'].to_s.start_with?('crazy-max/ghaction-github-runtime@') }, "#{suite} must expose the cache runtime")
  check(steps.none? { |step| step['run'].to_s.match?(/compose .*up .*--build/) }, "#{suite} must not rebuild images without cache")
end

# Kubernetes: the manifests are tested in a kind cluster built from the commit under test.
kube = YAML.load_file(File.join(ROOT, '.github/workflows/kubernetes.yml'))
kube_events = kube['on'] || kube[true]
check(kube_events.key?('workflow_dispatch'), 'Kubernetes CI must be runnable manually')
%w[pull_request push].each do |event|
  check(kube_events.dig(event, 'branches').sort == %w[dev main], "Kubernetes CI must run on #{event} to dev and main")
  check((%w[k8s/** .github/workflows/kubernetes.yml] - kube_events.dig(event, 'paths').to_a).empty?, "Kubernetes CI must run when k8s/ or the workflow change")
end
check(kube['permissions'] == { 'contents' => 'read' }, 'Kubernetes CI must use a read-only token')
kube_steps = kube.fetch('jobs').fetch('kind').fetch('steps')
kind_step = kube_steps.find { |step| step['uses'].to_s.start_with?('helm/kind-action@') }
check(!kind_step.nil? && kind_step['uses'].match?(/@v\d+\.\d+\.\d+\z/), 'kind-action must be pinned to an exact version')
check(kind_step.dig('with', 'version').to_s.match?(/\Av\d+\.\d+\.\d+\z/), 'The kind version must be pinned')
check(kind_step.dig('with', 'config') == 'k8s/kind-config.yaml', 'The CI cluster must use k8s/kind-config.yaml')
check(kube_steps.any? { |step| step['run'].to_s.include?('scripts/ci-build-images.sh') }, 'Kubernetes CI must build images with cache and retry')
check(kube_steps.any? { |step| step['uses'].to_s.start_with?('crazy-max/ghaction-github-runtime@') }, 'Kubernetes CI must expose the cache runtime')
check(kube_steps.any? { |step| step['run'].to_s.include?('kubectl apply -k') }, 'Kubernetes CI must apply the Kustomize manifests')
check(kube_steps.any? { |step| step['run'].to_s.include?('rollout status') }, 'Kubernetes CI must wait for the rollouts')
check(kube_steps.any? { |step| step['run'].to_s.include?('delete pod') }, 'Kubernetes CI must prove that a deleted pod is replaced')
check(kube_steps.any? { |step| step['if'] == 'failure()' && step['run'].to_s.include?('logs') }, 'Kubernetes CI must print logs on failure')
check(kube_steps.none? { |step| step['continue-on-error'] }, 'Kubernetes CI must propagate failures')
check(kube_steps.none? { |step| step['uses'].to_s.start_with?('actions/upload-artifact@') }, 'CI must use logs only')

kustomization = YAML.load_file(File.join(ROOT, 'k8s/kustomization.yaml'))
check(kustomization['namespace'] == 'ctv', 'Kubernetes objects must live in the ctv namespace')
app_images = kustomization.fetch('images').to_h { |image| [image['name'], image] }
%w[backend flights-service frontend].each do |service|
  image = app_images["ghcr.io/acostaf12/ctv-#{service}"]
  check(!image.nil? && !image['newTag'].to_s.empty?, "kustomization.yaml must set the #{service} image tag")
end
manifests = kustomization.fetch('resources').flat_map { |file| YAML.load_stream(File.read(File.join(ROOT, 'k8s', file))).compact }
workloads = manifests.select { |doc| %w[Deployment StatefulSet].include?(doc['kind']) }
check(workloads.map { |doc| doc.dig('metadata', 'name') }.sort == %w[backend backend-postgres flights-postgres flights-service frontend], 'Missing Kubernetes workloads')
workloads.each do |doc|
  name = doc.dig('metadata', 'name')
  pod = doc.dig('spec', 'template', 'spec')
  check(pod.dig('securityContext', 'runAsNonRoot') == true, "#{name} must run as non-root")
  (pod['initContainers'].to_a + pod.fetch('containers')).each do |container|
    image = container.fetch('image')
    # Application images get their tag from kustomization.yaml; third-party images are pinned here.
    check(image.start_with?('ghcr.io/acostaf12/ctv-') || (image.match?(%r{:[^/]+\z}) && !image.end_with?(':latest')), "k8s/#{name} must pin #{image}")
    check(container.dig('resources', 'requests') && container.dig('resources', 'limits'), "#{name}/#{container['name']} must set requests and limits")
    check(container.dig('securityContext', 'allowPrivilegeEscalation') == false, "#{name}/#{container['name']} must not escalate privileges")
  end
  pod.fetch('containers').each do |container|
    check(container['readinessProbe'] && container['livenessProbe'], "#{name} must define readiness and liveness probes")
  end
end
check(workloads.select { |doc| doc['kind'] == 'StatefulSet' }.all? { |doc| doc.dig('spec', 'volumeClaimTemplates') }, 'Databases must keep their data in a PersistentVolumeClaim')
%w[backend frontend].each do |name|
  check(workloads.find { |doc| doc.dig('metadata', 'name') == name }.dig('spec', 'replicas').to_i >= 2, "#{name} must run at least 2 replicas")
end
flights_svc = manifests.find { |doc| doc['kind'] == 'Service' && doc.dig('metadata', 'name') == 'flights-service' }
check(flights_svc.dig('spec', 'type') == 'ClusterIP', 'flights-service must only be reachable inside the cluster')
secret = manifests.find { |doc| doc['kind'] == 'Secret' }
check(secret.dig('stringData', 'JWT_SECRET').to_s.bytesize >= 32, 'The demo JWT_SECRET must have at least 32 bytes')
kind_ports = YAML.load_file(File.join(ROOT, 'k8s/kind-config.yaml')).fetch('nodes').flat_map { |node| node['extraPortMappings'].to_a }
{ 'backend' => 8080, 'frontend' => 8090 }.each do |name, host_port|
  svc = manifests.find { |doc| doc['kind'] == 'Service' && doc.dig('metadata', 'name') == name }
  node_port = svc.dig('spec', 'ports', 0, 'nodePort')
  check(kind_ports.any? { |m| m['containerPort'] == node_port && m['hostPort'] == host_port }, "kind must map #{name} to localhost:#{host_port}")
end

puts 'CI contract checks passed'
