# Publish Documentation to GitHub Pages action

This action publishes documentation by pushing it to a branch, by default `gh-pages`.

#### Inputs

Only `from` is required, although `version` will be if `publish-to` contains `$verison` or `$latest` or `message`
contains `$version`.

* `from` - the location of the docs to publish
* `publish-to` - What directory to put the docs in. A comma separated list of folder names.  `$version` will be replaced with the passed version
  (or error if it isn't passed).  `$latest` will be replaced with `snapshot` for snapshot versions (`snapshot` is in the string in any case), 
  `release` otherwise (or error if `version` isn't passed), unless set using `latests`. 
  Basic escaping is done as well: `\,` will be replaced with `,`, `\$` with `$`, and `\\` with `\` in items (after the version and latest substitutions).  
  By default, is `$version,$latest` if `version` is passed or `.` otherwise.  Items that start with `!` will only be included for release versions, 
  and items that start with `?` will only be included on snapshot versions (all will be included if version is not specified).
  
  For example, you could use `$latest,!old_releases/$version` to publish to `snapshot` on snapshot versions, and `old_releases/$verison` and `release` on 
  release versions, i.e. for use with the Dokka versioning plugin.
* `version` - the version string to use if `publish-to` contains `$version` or `$latest`, or if `message`
  contains `$version`. Required if so, optional and unused otherwise.
* `latests` - the values to replace `latest` with in `publish-to`.  The value for snapshots, `|`, then the value for releases.  Default is `snapshot|release`.
* `branch` - the branch to push docs to. By default `gh-pages`.
* `message` - the message for the docs commit.  `$version` will be substituted for the passed version. Default
  is `"Docs for \$version"` is `version` is passed, or `"Docs update"` if not.
* `restore` - whether to save and restore the working directory after pushing docs. True by default. Will only not
  restore if equal to `false` (non case sensitive).
* `author-name` - the author name for the commit.  The triggering user, by default.
* `author-email` - the author email for the commit.  The triggering user's GitHub noreply address, by default.

This gives two modes of operation: one where `version` is specified, `publish-to` can use `version` or `version+latest`,
and `message` can use `$version`, and one where `version` is not specified and `pubish-to` must be a path and `message`
can't use `$version`.