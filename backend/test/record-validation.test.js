import test from 'node:test';
import assert from 'node:assert/strict';
import { validateUserRecord, isApprovalTransitionAllowed } from '../src/record-validation.js';

const validTask = {
  repo: 'owner/project', branch: 'main', title: 'Test task', prompt: 'Run safe tests',
  status: 'queued', currentStepIndex: 0, totalSteps: 2, progressPercent: 0,
  lastCompletedStep: '', lastResultSummary: '', lastError: '', modifiedFiles: [], nextSteps: []
};

const validApproval = {
  taskId: 'task_1', tier: 2, actionType: 'git_push', title: 'Push changes',
  description: 'Request review before pushing', payloadPreview: '', targetResource: 'owner/project',
  status: 'pending', decisionNote: ''
};

test('validates user-owned task schema and rejects unsafe progress values', () => {
  assert.equal(validateUserRecord('tasks', validTask), true);
  assert.equal(validateUserRecord('tasks', { ...validTask, progressPercent: 101 }), false);
  assert.equal(validateUserRecord('tasks', { ...validTask, currentStepIndex: -1 }), false);
  assert.equal(validateUserRecord('tasks', { ...validTask, modifiedFiles: ['x'.repeat(501)] }), false);
});

test('validates approvals and prevents client-asserted executed status', () => {
  assert.equal(validateUserRecord('approvals', validApproval), true);
  assert.equal(validateUserRecord('approvals', { ...validApproval, status: 'executed' }), false);
  assert.equal(validateUserRecord('approvals', { ...validApproval, tier: 1 }), false);
});

test('approval state machine allows only pending -> approved/rejected -> reported', () => {
  assert.equal(isApprovalTransitionAllowed(null, 'pending'), true);
  assert.equal(isApprovalTransitionAllowed(null, 'approved'), false);
  assert.equal(isApprovalTransitionAllowed('pending', 'approved'), true);
  assert.equal(isApprovalTransitionAllowed('pending', 'rejected'), true);
  assert.equal(isApprovalTransitionAllowed('approved', 'execution_reported'), true);
  assert.equal(isApprovalTransitionAllowed('rejected', 'approved'), false);
  assert.equal(isApprovalTransitionAllowed('execution_reported', 'approved'), false);
  assert.equal(isApprovalTransitionAllowed('approved', 'executed'), false);
});

test('operation logs accept known categories and error outcomes', () => {
  const log = { category: 'test', tier: 1, action: 'Run tests', details: '', outcome: 'error', repo: 'owner/project' };
  assert.equal(validateUserRecord('operation_logs', log), true);
  assert.equal(validateUserRecord('operation_logs', { ...log, outcome: 'anything' }), false);
});

const validStudioProject = {
  projectName: 'sample-app',
  environment: 'WEB_REACT_NEXT',
  primaryLanguage: 'TYPESCRIPT',
  description: 'A small sample',
  files: [
    { path: 'src/index.ts', language: 'TYPESCRIPT', content: 'export {}', purpose: 'entry', isEntrypoint: true },
    { path: '.env.example', language: 'TEXT', content: 'PUBLIC_API_URL=', purpose: 'sample config', isEntrypoint: false }
  ],
  buildInstructions: 'npm run build',
  runInstructions: 'npm run dev',
  testInstructions: 'npm test'
};


test('rejects Unicode-normalized duplicate paths and non-portable Windows names', () => {
  const composed = 'caf\u00e9.txt';
  const decomposed = 'cafe\u0301.txt';
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [
      { ...validStudioProject.files[0], path: composed },
      { ...validStudioProject.files[1], path: decomposed }
    ]
  }), false);

  for (const unsafePath of ['CON.txt', 'src/NUL', 'folder/trailing.', 'folder/trailing ', 'src/bad?.ts']) {
    assert.equal(validateUserRecord('studio_projects', {
      ...validStudioProject,
      files: [{ ...validStudioProject.files[0], path: unsafePath }]
    }), false, unsafePath);
  }
});

test('validates durable Studio projects and rejects unsafe file manifests', () => {
  assert.equal(validateUserRecord('studio_projects', validStudioProject), true);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [{ ...validStudioProject.files[0], path: '../outside.ts' }]
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [
      validStudioProject.files[0],
      { ...validStudioProject.files[1], path: 'SRC/INDEX.TS' }
    ]
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [{ ...validStudioProject.files[0], path: '.env.local' }]
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [{ ...validStudioProject.files[0], path: 'C:/private/config.ts' }]
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [{ ...validStudioProject.files[0], path: 'credentials.json' }]
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: validStudioProject.files.map((file, index) => ({ ...file, isEntrypoint: index === 0 || index === 1 }))
  }), false);
  assert.equal(validateUserRecord('studio_projects', {
    ...validStudioProject,
    files: [{ ...validStudioProject.files[0], content: 'x'.repeat(48001) }]
  }), false);
});
