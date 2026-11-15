const boundedString = (value, min, max) => typeof value === 'string' && value.length >= min && value.length <= max;
const stringList = (value, maxItems) => Array.isArray(value) && value.length <= maxItems && value.every(item => boundedString(item, 0, 500));

export function validateUserRecord(collection, data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) return false;
  if (collection === 'settings') return boundedString(data.activeRepo, 0, 128) &&
    boundedString(data.activeBranch, 0, 128) && ['gemini', 'openai'].includes(data.aiProvider) &&
    boundedString(data.aiModel, 1, 64) && boundedString(data.backendUrl, 0, 300) &&
    typeof data.darkTheme === 'boolean' && typeof data.autoExecuteTier1 === 'boolean';
  if (collection === 'tasks') return boundedString(data.repo, 0, 128) &&
    boundedString(data.branch, 0, 128) && boundedString(data.title, 1, 200) &&
    boundedString(data.prompt, 1, 2000) &&
    ['queued', 'running', 'awaiting_approval', 'completed', 'failed', 'paused'].includes(data.status) &&
    Number.isSafeInteger(data.currentStepIndex) && data.currentStepIndex >= 0 &&
    Number.isSafeInteger(data.totalSteps) && data.totalSteps >= 0 &&
    Number.isSafeInteger(data.progressPercent) && data.progressPercent >= 0 && data.progressPercent <= 100 &&
    boundedString(data.lastCompletedStep, 0, 300) && boundedString(data.lastResultSummary, 0, 2000) &&
    boundedString(data.lastError, 0, 1000) && stringList(data.modifiedFiles, 30) && stringList(data.nextSteps, 30);
  if (collection === 'approvals') return boundedString(data.taskId, 0, 128) &&
    [2, 3].includes(data.tier) &&
    ['git_push', 'create_pr', 'staging_deploy', 'service_config', 'db_sql', 'db_rls_grants', 'db_migration', 'merge_pr', 'prod_deploy'].includes(data.actionType) &&
    boundedString(data.title, 1, 200) && boundedString(data.description, 1, 2000) &&
    boundedString(data.payloadPreview, 0, 8000) && boundedString(data.targetResource, 0, 300) &&
    ['pending', 'approved', 'rejected', 'execution_reported'].includes(data.status) &&
    boundedString(data.decisionNote, 0, 500);
  if (collection === 'chat_messages') return ['user', 'assistant', 'system'].includes(data.role) &&
    boundedString(data.content, 1, 12000) && boundedString(data.repoContext, 0, 128) &&
    boundedString(data.linkedTaskId, 0, 128);
  if (collection === 'operation_logs') return ['agent', 'github', 'security', 'ai', 'backend', 'test'].includes(data.category) &&
    Number.isSafeInteger(data.tier) && data.tier >= 1 && data.tier <= 3 &&
    boundedString(data.action, 1, 200) && boundedString(data.details, 0, 3000) &&
    ['success', 'failed', 'error', 'blocked', 'pending'].includes(data.outcome) && boundedString(data.repo, 0, 128);
  return false;
}

export function isApprovalTransitionAllowed(priorStatus, nextStatus) {
  if (!priorStatus) return nextStatus === 'pending';
  if (priorStatus === 'pending') return ['pending', 'approved', 'rejected'].includes(nextStatus);
  if (priorStatus === 'approved') return ['approved', 'execution_reported'].includes(nextStatus);
  return false;
}
