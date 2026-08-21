# Checklist manual — Deploy de demo en Azure

Esto es todo lo que hay que hacer **una sola vez**, a mano (no lo puede hacer CI). Requiere tener `az cli` instalado y logueado (`az login`) con la cuenta que activó el beneficio de Azure for Students.

## 1. Variables de referencia

Usá estos valores (coinciden con lo hardcodeado en `.github/workflows/deploy.yml`), ya confirmados como funcionales para esta suscripción:

```bash
RESOURCE_GROUP=ctv-demo-rg
VM_NAME=ctv-demo-vm
LOCATION=northcentralus
SIZE=Standard_B2as_v2   # recomendado por el propio portal de Azure — funcionó donde D2s_v3/D2as_v7/D2as_v4 fallaron por capacidad
```

Requisito previo (una sola vez, si no lo hiciste ya): registrar los resource providers, si no las llamadas de creación/cuota devuelven vacío o fallan raro:
```bash
az provider register --namespace Microsoft.Compute
az provider register --namespace Microsoft.Network
# esperar 1-5 min y confirmar con:
az provider show --namespace Microsoft.Compute --query registrationState -o tsv
az provider show --namespace Microsoft.Network --query registrationState -o tsv
```

### Cómo se llegó a `northcentralus` / `Standard_D2s_v3` (por si hay que repetir el proceso)

Esta suscripción de Azure for Students tiene **3 capas de restricción independientes**, y cada una da un error distinto — hay que resolverlas en orden:

1. **Regiones permitidas** (Azure Policy `sys.regionrestriction`, fija para la suscripción): solo `westus3`, `southafricanorth`, `mexicocentral`, `chilecentral`, `northcentralus`. Cualquier otra región falla con `RequestDisallowedByAzure` / "best available regions", sin importar el tamaño. Para volver a consultarla:
   ```powershell
   az policy assignment list --query "[?policyDefinitionId=='/providers/Microsoft.Authorization/policyDefinitions/b86dabb9-b578-4d7b-b842-3b45e95769a1']"
   ```

2. **Cuota por familia de VM** (por región, varía): la mayoría de las familias "modernas" (`Dasv7`, etc.) tienen cuota **0**. Para ver qué familias sí tienen cuota > 0 en una región de la lista anterior:
   ```powershell
   az vm list-usage --location <region> -o table
   ```
   Buscar filas con `Limit` > 0 (columna derecha). En esta cuenta, `Standard DSv3 Family` = 4 y `Standard Bsv2/Basv2 Family` = 10 vCPUs, con un tope total de `Total Regional vCPUs` = 6 (o sea: como mucho una VM de 2 vCPUs de una sola familia). Este error se ve como `QuotaExceeded ... Current Limit: 0`.

3. **Capacidad física real** (dinámica, no queda reflejada en `list-skus` ni en la cuota): aunque la familia tenga cuota, la región puntual puede no tener stock físico de ese SKU ahí en ese momento — error `SkuNotAvailable ... Capacity Restrictions`. Pasó con `Standard_D2s_v3` en `chilecentral` (región chica/nueva); se resolvió cambiando a `northcentralus` (región grande y consolidada), sin cambiar el tamaño. No hay forma de chequear esto de antemano — solo probando el `az vm create`.

## 2. Crear resource group + VM

```bash
az group create --name $RESOURCE_GROUP --location $LOCATION

az vm create \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --image Ubuntu2404 \
  --size $SIZE \
  --admin-username azureuser \
  --generate-ssh-keys \
  --public-ip-sku Standard
```

Guardá la IP pública que devuelve (`publicIpAddress`) — es el `VM_HOST`. La clave privada generada (`~/.ssh/id_rsa` por defecto, si no tenías una) es el `VM_SSH_KEY`.

## 3. Abrir puertos en el NSG

SSH (22) ya queda abierto por default al crear la VM. Para el resto (en PowerShell — las prioridades de NSG deben estar entre 100 y 4096, así que van fijas, no calculadas a partir del puerto):

```powershell
az vm open-port --resource-group ctv-demo-rg --name ctv-demo-vm --port 8090 --priority 310
az vm open-port --resource-group ctv-demo-rg --name ctv-demo-vm --port 8080 --priority 320
az vm open-port --resource-group ctv-demo-rg --name ctv-demo-vm --port 3001 --priority 330
az vm open-port --resource-group ctv-demo-rg --name ctv-demo-vm --port 9411 --priority 340
az vm open-port --resource-group ctv-demo-rg --name ctv-demo-vm --port 5601 --priority 350
```

(`8081` de flights-service queda cerrado a propósito — solo se usa entre contenedores.)

## 4. Instalar Docker en la VM

```bash
ssh azureuser@<IP_VM> 'curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker $USER'
```

Cerrá y reabrí la sesión SSH después de esto para que el grupo `docker` tome efecto.

## 5. Copiar los archivos de compose a la VM

Desde la raíz del repo, en tu máquina:

IP_VM=64.236.199.81

```bash
ssh azureuser@64.236.199.81 'sudo mkdir -p /opt/ctv && sudo chown azureuser:azureuser /opt/ctv'
scp docker-compose.yml docker-compose.prod.yml azureuser@64.236.199.81:/opt/ctv/
scp -r monitoring azureuser@64.236.199.81:/opt/ctv/
```

(`/opt` requiere root, por eso el `sudo` — el `chown` deja la carpeta con dueño `azureuser` para que el `scp` de después no necesite privilegios.)

Repetir el `scp` cada vez que cambien estos archivos (el workflow de deploy no los sincroniza, solo hace `pull` + `up -d` de las imágenes).

## 6. Auto-shutdown como red de seguridad

```bash
az vm auto-shutdown --resource-group $RESOURCE_GROUP --name $VM_NAME --time 2300
```

Esto apaga la VM todos los días a las 23:00 (hora del server) si te olvidaste de apagarla manualmente después de una demo.

## 7. Hacer públicos los packages en GHCR

Recién después del **primer push a `main`** que dispare los workflows de `backend.yml`/`flights-service.yml`/`frontend.yml` (una vez que este branch se mergee), van a aparecer 3 packages nuevos en:

`https://github.com/AcostaF12?tab=packages`

Para cada uno (`ctv-backend`, `ctv-flights-service`, `ctv-frontend`): entrar → **Package settings** → **Change visibility** → **Public**. Sin esto, la VM no va a poder hacer `docker compose pull` (los packages nacen privados).

## 8. Cargar secrets en GitHub

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Valor |
|---|---|
| `AZURE_CREDENTIALS` | JSON de un Service Principal (ver paso 9) |
| `VM_HOST` | IP pública de la VM (paso 2) |
| `VM_USER` | `azureuser` |
| `VM_SSH_KEY` | Contenido de la clave privada SSH (paso 2) |

## 9. Crear el Service Principal para `AZURE_CREDENTIALS`

```bash
az ad sp create-for-rbac \
  --name "ctv-demo-deploy" \
  --role contributor \
  --scopes /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/$RESOURCE_GROUP \
  --sdk-auth
```

Copiá el JSON completo que imprime y pegalo como valor del secret `AZURE_CREDENTIALS`. (`<SUBSCRIPTION_ID>` sale de `az account show --query id -o tsv`.)

## 10. Probar

1. Actions → `Deploy Demo (Azure VM)` → Run workflow → `deploy`. Debería: prender la VM, esperar SSH, hacer pull+up de las imágenes, y pasar el healthcheck del backend.
2. Entrar a `http://<IP_VM>:8090` (frontend) y `http://<IP_VM>:3001` (Grafana) para confirmar que todo levantó.
3. Actions → `Deploy Demo (Azure VM)` → Run workflow → `stop`. Confirmar en el portal de Azure que la VM queda en estado **Deallocated**.
