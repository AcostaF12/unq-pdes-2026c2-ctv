const common = {
  paths: ['features/**/*.feature'],
  import: ['features/step_definitions/**/*.js', 'features/support/**/*.js'],
  publishQuiet: true,
}

export default {
  ...common,
  format: ['progress', 'html:reports/cucumber-report.html'],
}

export const ci = {
  ...common,
  format: ['progress'],
}
