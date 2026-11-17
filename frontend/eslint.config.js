// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // Allow explicit type annotations on initialized properties/variables
      '@typescript-eslint/no-inferrable-types': 'off',
      // Allow legacy *ngIf / *ngFor alongside @if / @for in inline templates
      '@angular-eslint/prefer-standalone': 'error',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended],
    rules: {
      // Allow traditional *ngIf / *ngFor / *ngSwitch without forcing @if / @for
      '@angular-eslint/template/prefer-control-flow': 'off',
      // Do not block CI on decorative/group <label> elements without `for`
      '@angular-eslint/template/label-has-associated-control': 'off',
    },
  },
]);
