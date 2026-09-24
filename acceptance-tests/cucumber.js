export default {
  default: {
    paths: ['features/**/*.feature'],
    import: ['features/step_definitions/**/*.js', 'features/support/**/*.js'],
    format: ['progress', 'html:reports/cucumber-report.html'],
    publishQuiet: true,
  },
}
