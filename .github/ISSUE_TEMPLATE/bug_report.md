---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

**Checklist**

Make sure that you've checked all the items below.

- [ ] Bot has the following permissions:
  - MESSAGE_ADD_REACTION
  - MESSAGE_EXT_EMOJI
  - MESSAGE_READ/WRITE
  - VIEW_CHANNEL
- [ ] If using `JDABuilder.createLight()`, you added the following gateway intents:
    - GUILD_MESSAGES
    - GUILD_MESSAGE_REACTIONS
- [ ] If using `.setRemoveOnReact(true)`, you have the following permission:
    - MESSAGE_MANAGE
- [ ] PaginationUtils is up-to-date.
- [ ] You have activated the library as descripted in the README.


**Library info**

What libraries versions are you using.
- JDA version X.XXX
- Pagination-Utils version X.XXX


**Describe the bug**

A clear and concise description of what the bug is.


**To Reproduce**

Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error


**Expected behavior**

A clear and concise description of what you expected to happen.


**Screenshots**

If applicable, add screenshots to help explain your problem.


**Additional context**

Add any other context about the problem here.
