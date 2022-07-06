module.exports = {
  automock: false,
  setupFiles: ['jest-localstorage-mock'],
  moduleFileExtensions: ['vue', 'js', 'json'],
  testEnvironment: 'jsdom',
  testEnvironmentOptions: {
    customExportConditions: ['node', 'node-addons'],
    url: 'https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a'
  },
  transform: {
    "^.+\\.vue$": "@vue/vue3-jest",
    "^.+\\js$": "babel-jest"
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  }
};
