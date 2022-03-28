module.exports = {
  moduleFileExtensions: ['vue', 'js', 'json'],
  testEnvironment: 'jsdom',
  transform: {
    '^.+\\.vue$': "@vue/vue3-jest",
    "^.+\\js$": "babel-jest"
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  }
}
