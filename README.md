# Publish Documentation to GitHub Pages action

This action publishes documentation by pushing it to a branch, by default `gh-pages`.

#### Inputs

Only `from` is required, although `version` will be if `publish-to` is `version` or `version+latest` or `message`
contains `$version`.

* `from` - the location of the docs to publish
* `publish-to` - What directory to put the docs in. Can be a folder name, `version` to use the passed version,
  or `version+latest` to use the passed version and a latest folder (`snapshot` for snapshot versions, `release`
  otherwise). By default, is `version+latest` if `version` is passed or `.` otherwise.
* `version` - the version string to use if `publish-to` is `version` or `version+latest`, or if `message`
  contains `$version`. Required if so, optional and unused otherwise.
* `branch` - the branch to push docs to. By default `gh-pages`.
* `message` - the message for the docs commit.  `$version` will be substituted for the passed version. Default
  is `"Docs for \$version"` is `version` is passed, or `"Docs update"` if not.
* `restore` - whether to save and restore the working directory after pushing docs. True by default. Will only not
  restore if equal to `false` (non case sensitive).

This gives two modes of operation: one where `version` is specified, `publish-to` can use `version` or `version+latest`,
and `message` can use `$version`, and one where `version` is not specified and `pubish-to` must be a path and `message`
can't use `$version`.