# Contributing to DIVID-DJ

Here are the guidelines, we would like you to follow (*Note*:  These guidelines are highly inspired from [AngularJS](https://github.com/angular/angular/blob/master/CONTRIBUTING.md)):

- [Coding Rules](#rules)
- [Submission Guidelines](#submit)
- [Commit Message Guidelines](#commit)



## <a name="rules"></a> Coding Rules

* All features or bug fixes **must be tested** by one or more specs (unit-tests).
* All public API methods **must be documented**.
* Code should be formatted according to **Scalariform** conventions. An automated formatter is available and will be executed when compiling the code and tests.


## <a name="submit"></a> Submission Guidelines

**Things to never ever do (or at least try to avoid):**

* Don't make changes to master, always start a new branch.
* Don’t merge. It messes up the commit history.
* Don’t pull upstream without a rebase (see above). git fetch and then rebase instead (or equivalently, git pull upstream master --rebase).
* Don’t use git commit -a. You could silently commit something regrettable. Use -p instead.

Read section ["A git recipe for Angular repositories
"](https://docs.google.com/document/d/1h8nijFSaa1jG_UE8v4WP7glh5qOUXnYtAtJh_gwOQHI/edit) to understand the git work flow we prefer. You may also want to read [this](http://codeinthehole.com/writing/pull-requests-and-other-good-practices-for-teams-using-github/).


## <a name="commit"></a> Commit Message Guidelines

### Goals

* allow generating CHANGELOG.md by script
* allow ignoring commits (like formatting)
* provide better information when browsing the history

### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional.

Any line of the commit message cannot be longer 100 characters! This allows the message to be easier
to read on GitHub as well as in various git tools.

### Revert
If the commit reverts a previous commit, it should begin with `revert: `, followed by the header of the reverted commit. In the body it should say: `This reverts commit <hash>.`, where the hash is the SHA of the commit being reverted.

### Type
Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation

### Scope
The scope could be anything specifying place of the commit change. For example
`Compiler`, `ElementInjector`, etc.

### Subject
The subject contains succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize first letter
* no dot (.) at the end

### Body
Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behavior.

### Footer
The footer should contain any information about **Breaking Changes** and is also the place to
reference GitHub issues that this commit **Closes**.

**Breaking Changes** should start with the word `BREAKING CHANGE:` with a space or two newlines. The rest of the commit message is then used for this.

A detailed explanation can be found in this [document][commit-message-format].

[commit-message-format]: https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#
