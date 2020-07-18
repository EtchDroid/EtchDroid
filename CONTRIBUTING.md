# How to contribute

Thanks for taking your time to contribute to EtchDroid!

Here's a few things to keep in mind so that your contributions will be happily
accepted.


## Code of conduct

EtchDroid follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/1/4/code-of-conduct/)

In general:

- Try to be helpful.
- Do not take critiques to code personally. See if you can improve it or explain
  your point better instead.
- Don't Be A Dick™️


## Your first code contribution

Want to help out with EtchDroid?

Look for issues tagged with "good first issue". Those are issues that should be
pretty simple to address, so you can get started with them and become familiar
with the codebase.

If you're more experienced, you may want to give a look to the "help wanted"
issues.


## Reporting bugs

Before opening a new issue, make sure someone else has not reported it already.

Also, make sure the bug isn't a device or USB adapter issue. If the app fails
writing your ISO, try again while your device is on a table, and make sure the
screen doesn't turn off. That does the trick very often.

When reporting bugs, you will be shown a template that you will have to fill in.
Do it. I need that information to help you troubleshoot. I can't troubleshoot
issues that cannot be reproduced.


## Suggesting features

As with issues, make sure no one else has already suggested the same feature.

Note that, while I do accept feature suggestions, I don't always have time to
work on it. This is a personal, free time project.

In general, if you can implement it yourself, that would be a lot better.

I will not be accepting features suggestions that involve requesting root
privileges or, in general, that are out of the scope of this app.


## Translating

If you want to help translating EtchDroid in your language, head over to the
[Weblate page](https://hosted.weblate.org/engage/etchdroid/).

If you have any issues, let me know: there's a [thread](https://github.com/EtchDroid/EtchDroid/issues/18)
dedicated to translations you can join.

Please beware that translations should **only** be contributed using Weblate!
Pull requests for translations will be rejected. Your changes will be credited:
Weblate will commit the changes using the email address you used to register.

When you're done translating, make sure you update [`about.xml`](https://github.com/EtchDroid/EtchDroid/blob/develop/app/src/main/res/xml/about.xml)
with your info, so you will be credited in the app's "About" page. You can send
a pull request for that.

Weblate hosting is kindly provided for free: if you like it, you can buy them a
coffee: https://weblate.org/en/donate/


## Sending pull requests

Here's a few rules to follow to make sure your pull requests get merged
painlessly:

- Before submitting a big change, open an issue first and let me know what's
  your plan. This will avoid wasting your time in case your change conflicts
  with the project goals.

- Do not reformat files. Or at least, ask first. Pull requests in which more
  than 10% changes are simply fixing indentations, renaming variables
  or sorting stuff around will likely be rejected. They take time to review
  and they don't add anything useful to the project.

- Remember: **Do not fix what's not broken**

- The linter is configured: use it. Before submitting, run `./gradlew lint`
  and check the result. Feel free to ignore translation issues or preexisting
  warnings.

- Test your changes. This involves running unit tests (either from Android Studio
  or with `./gradlew test`), running the app and flashing a test image. I would
  like to automate this part as well at some point, but for the time being,
  [technical limitations](https://github.com/magnusja/libaums/issues/209)
  do not make it possible.

- If I request changes, do not take it personally. If you think I'm wrong,
  explain why. If we still do not agree, try to work to find a compromise or remove
  that change. Disagreements happen and the solution is compromise. Acting
  passive-aggressive won't help.

