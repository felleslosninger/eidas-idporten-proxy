# eidas-idporten-proxy

Idporten-proxy as specific proxy for service for Norway from ID-porten.
Cache for LightProtocol requests and responses. Must match names that the eu-eidas-proxy uses.

## Configuration

Idporten-proxy is configured with the following environment variables:
eidas.acr.supported-acr-values - Eidas ACR values supported by the eIDAS proxy. the lowes acr level must come first and
in subsequent order
eidas.acr.acr-value-map - maps ipdorten acr level to eidas acr levels.

## Sequence diagrams

### Norwegian citizen

```mermaid  
sequenceDiagram
    autonumber
    participant NEP as eu-eidas-proxy
    participant RED as redis
    participant SEP as idporten-proxy
    participant IL as ID-Porten
    NEP ->> RED: put LightProtocol request
    NEP ->> SEP: LightToken
    SEP ->> RED: get LightProtocol request
    SEP ->> SEP: map to OIDC
    SEP ->> IL: OIDC (acr: idporten-eidas-loa-x(?), scope: eidas:<tbd>)
    IL -->> SEP: code response
    SEP ->> IL: getToken
    IL -->> SEP: token response
    SEP ->> SEP: map to LightProtocol response
    SEP -->> NEP: LightProtocol response

```    
