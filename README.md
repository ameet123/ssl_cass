### Cassandra SSL with Spring Data

#### 1. JKS Creation
Create a starter Java Key store with a private key
```shell script
 keytool.exe -genkey -keyalg RSA -alias "clientCass" -keystore client_cass.ks -storepass changeit -keypass changeit
```
#### 2. Export Client certificate
```shell script
keytool.exe -export -alias "clientCass" -file client.crt -keystore .\client_cass.ks -storepass changeit -keypass changeit
```
#### 3. Import client certificate into a new Trustore of format JKS
```shell script
keytool -importcert -v -trustcacerts -alias "clientCass" -file .\client.crt -keystore .\client_cass_trust.ks -storepass changeit
```
new trust-store : `client_cass_trust.ks`
#### 4. Import server certificate
```shell script
keytool -importcert -v -trustcacerts -alias "serverCass" -file .\rootCa.crt -keystore .\client_cass_trust.ks -storepass changeit
```

Note: the given root CA was incorrect. It was not the issuer's cert , rather the server cert.
We can get issuer's cert by
1. port forward 9042 to local
2. open browser and hit https://localhost:9042
3. collect issuer's cert and save it to file

#### 5. Convert format from JKS to PKCS12
```shell script
keytool.exe -importkeystore -srckeystore .\client_cass_trust.ks -destkeystore client_trust_pkcs12.ks -deststoretype PKCS12 -srcstorepass changeit -deststorepass changeit
```
