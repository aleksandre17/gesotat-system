# Required GitHub branch protection

Repository files cannot prevent an administrator from bypassing GitHub. Configure
the `main` and/or `master` protected branch in GitHub with all of the following:

1. Require a pull request before merging.
2. Require at least one approval and require review from CODEOWNERS.
3. Dismiss stale approvals when new commits are pushed.
4. Require conversation resolution.
5. Require the status check `architecture-governance` to pass and be up to date.
6. Do not allow force pushes or branch deletion.
7. Apply the rules to administrators and disallow bypass except an audited,
   time-bounded emergency procedure.
8. Require signed commits and linear history where repository operating policy
   supports them.

The workflow is fail-closed for repository evidence and review-range coverage. It
does not prove runtime correctness or production readiness; applicable behavioral,
security, migration and release checks remain mandatory.
