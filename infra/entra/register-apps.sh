#!/usr/bin/env bash
# Registers the API and SPA apps in the signed-in tenant. Run once with: az login --tenant <tenant-id>
set -euo pipefail

APP_URL="${1:-http://localhost:8080}"
TENANT_ID="$(az account show --query tenantId -o tsv)"
DOMAIN="$(az rest --method get --url https://graph.microsoft.com/v1.0/domains --query "value[?isDefault].id | [0]" -o tsv)"
uuid() { cat /proc/sys/kernel/random/uuid; }

API_ID="$(az ad app create --display-name "Notsureance API" --sign-in-audience AzureADMyOrg --query appId -o tsv)"
az ad app update --id "$API_ID" --identifier-uris "api://$API_ID"

cat > /tmp/notsureance-api.json <<JSON
{"requestedAccessTokenVersion": 2, "oauth2PermissionScopes": [
  {"id": "$(uuid)", "value": "claims.read", "type": "User", "isEnabled": true,
   "adminConsentDisplayName": "Read claims", "adminConsentDescription": "Read claims, policies and reports"},
  {"id": "$(uuid)", "value": "claims.write", "type": "User", "isEnabled": true,
   "adminConsentDisplayName": "Change claims", "adminConsentDescription": "File, decide and annotate claims"}]}
JSON
cat > /tmp/notsureance-roles.json <<JSON
[{"allowedMemberTypes": ["User"], "displayName": "Agent", "value": "Agent", "isEnabled": true, "id": "$(uuid)",
  "description": "Files claims for their own policyholders"},
 {"allowedMemberTypes": ["User"], "displayName": "Adjuster", "value": "Adjuster", "isEnabled": true, "id": "$(uuid)",
  "description": "Reviews and decides claims"},
 {"allowedMemberTypes": ["User"], "displayName": "Manager", "value": "Manager", "isEnabled": true, "id": "$(uuid)",
  "description": "Reads every claim, report and model metric"}]
JSON
az ad app update --id "$API_ID" --set api=@/tmp/notsureance-api.json
az ad app update --id "$API_ID" --app-roles @/tmp/notsureance-roles.json
az ad sp create --id "$API_ID" >/dev/null

SPA_ID="$(az ad app create --display-name "Notsureance Web" --sign-in-audience AzureADMyOrg --query appId -o tsv)"
OBJECT_ID="$(az ad app show --id "$SPA_ID" --query id -o tsv)"
az rest --method patch --url "https://graph.microsoft.com/v1.0/applications/$OBJECT_ID" \
  --body "{\"spa\": {\"redirectUris\": [\"$APP_URL\", \"http://localhost:4200\"]}}"
for scope in claims.read claims.write; do
  SCOPE_ID="$(az ad app show --id "$API_ID" --query "api.oauth2PermissionScopes[?value=='$scope'].id | [0]" -o tsv)"
  az ad app permission add --id "$SPA_ID" --api "$API_ID" --api-permissions "$SCOPE_ID=Scope"
done
az ad sp create --id "$SPA_ID" >/dev/null
az ad app permission admin-consent --id "$SPA_ID"

cat <<OUT

Add these to .env or the App Service settings:
ENTRA_TENANT_ID=$TENANT_ID
ENTRA_API_AUDIENCES=$API_ID,api://$API_ID
ENTRA_SPA_CLIENT_ID=$SPA_ID
ENTRA_API_SCOPES=api://$API_ID/claims.read,api://$API_ID/claims.write
DEMO_EMAIL_DOMAIN=$DOMAIN

Then create agent@, agent2@, adjuster@, adjuster2@ and manager@$DOMAIN and assign each the matching
app role under Enterprise applications > Notsureance API > Users and groups.
OUT
