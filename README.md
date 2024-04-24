# eidas-proxy
eIDAS-proxy as specific proxy for service for Norway from ID-porten

# Sequence diagrams

## Norwegian citizen

```mermaid  
sequenceDiagram
    autonumber
    participant NEP as eu-eidas-proxy
    participant RED as redis
    participant SEP as eidas-proxy
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