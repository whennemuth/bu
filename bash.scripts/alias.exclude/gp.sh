  local domain='team-bostonuniversity-apps'
  local sessionTokenID="OP_SESSION_$(echo $domain | sed 's/-/_/g')"
  /c/whennemuth/downloads/1password/op.exe signin --raw https://$domain.1password.com/ wrh@bu.edu