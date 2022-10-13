fixmd() {
  local code="$(cat <<EOF
  cd /c/whennemuth/workspaces/bu_workspace/bu-wordpress-cron &&
  git commit -a -m 'Trying another fix to the markdown file' &&
  git push origin master
EOF
)"
sh -c "$code"
}
