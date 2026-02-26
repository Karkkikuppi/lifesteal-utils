/** @type {import('jest').Config} */
module.exports = {
    testEnvironment: 'node',
    testMatch: ['<rootDir>/tests/**/*.test.ts'],
    extensionsToTreatAsEsm: ['.ts'],
    transform: {
        '^.+\\.ts$': [
            'ts-jest',
            {
                useESM: true,
                tsconfig: '<rootDir>/tsconfig.jest.json'
            }
        ]
    },
    setupFilesAfterEnv: ['<rootDir>/tests/setup.ts'],
    detectOpenHandles: true
}
