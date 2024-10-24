module.exports = {
  collectCoverage: true,
  collectCoverageFrom: ["**/src/components/**", "**/src/mixins/**"],
  coverageProvider: 'v8',
  coverageReporters: [["lcov", {"projectRoot": "../../../../"}], "json", "text"],
  moduleFileExtensions: ['vue', 'js', 'json'],
  testEnvironment: 'jsdom',
  testEnvironmentOptions: {
    customExportConditions: ['node', 'node-addons']
  },
  transform: {
    '^.+\\.vue$': "@vue/vue3-jest",
    "^.+\\js$": "babel-jest"
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  }
}
