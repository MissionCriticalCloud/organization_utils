# How to contribute to Cosmic

We like to encourage you to contribute to the repository.
This should be as easy as possible for you but there are a few things to consider when contributing.
The following guidelines for contribution should be followed if you want to submit a pull request.
If you lack knowledge or experience to complete some of these steps, but you are willing to learn and invest time, please submit your contribution anyway so we can possibly help you and still benefit both from the experience and code.

## How to prepare

* You need a [GitHub account](https://github.com/signup/free)
* Submit an [issue ticket](https://github.com/MissionCriticalCloud/cosmic/issues) for your issue if there is not one yet.
	* Describe the issue and include steps to reproduce when it's a bug.
	* Ensure to mention the exact version(s) that you know is affected.
  * If you plan on submitting a bug report, please submit debug-level logs along
    with the report using [gist](https://gist.github.com/) or some other paste
    service.

## Make Changes

* Fork the repository on GitHub to your account
* In your forked repository, create a topic branch for your upcoming patch. Do not work directly on the master branch.
	* Create a branch based on master; `git branch fix/my_contribution master`
	* Checkout the new branch with `git checkout fix/my_contribution`.
* Adhere to [defined code conventions](https://github.com/MissionCriticalCloud/checkstyle)
* Make commits of logical self-contained units and describe them properly.
* Submit tests to your patch / new feature so it can be tested easily.
* Rebase `fix/my_contribution` to include latest updates from `upstream/master`:
```
$ git remote add upstream https://github.com/MissionCriticalCloud/cosmic.git
$ git fetch upstream
$ git rebase upstream/master
```
* Assure nothing is broken by running all the tests (Unit, System, Integration).

## Submit Changes

* Push your changes to a topic branch in your fork of the repository.
* Open a pull request to the original repository and choose the right original branch you want to patch.
* If not done in commit messages (which you really should do) please reference and update your issue with the code changes.
* Even if you have write access to the repository, do not directly push or merge pull-requests. Let another team member review your pull request and approve.


# Additional Resources

* [MissionCriticalCloud code conventions](https://github.com/MissionCriticalCloud/checkstyle)
* [General GitHub documentation](http://help.github.com/)
* [GitHub pull request documentation](http://help.github.com/send-pull-requests/)
