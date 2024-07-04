package no.idporten.eidas.proxy.crypto;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = KeyStoreResourceLoader.class)
@ActiveProfiles("keystore")
class KeyStoreResourceLoaderTest {
    private static final char[] PASSWORD = "test".toCharArray();

    @Autowired
    private KeyStoreResourceLoader target;

    @Test
    void base64() {
        loadKeyStore(target.getResource("base64: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"));
    }

    @Test
    @SneakyThrows
    void mimeBase64() {
        loadKeyStore(target.getResource("base64:" +
                """
                            /u3+7QAAAAIAAAABAAAAAQAEdGVzdAAAAYZPGoMTAAAFATCCBP0wDgYKKwYBBAEqAhEBAQUABIIE
                        6dhXytdF54QuGTGsgOd2PyIn/Aj3/cl31vvfu+OyEmddsFsAGBrvRyJhiM7yw7f+LW7FSVadOi8t
                        /2vZZZHizCwDfut3qtWEYjz8r3LfwSNYOEUgBg/0m9E5yI0yp2U734DyljypFi4A5rmyuYOCD3sO
                        lFOACJdcXjsFGoryKaUBcvG/pLYTXObIYVPVi5VdXovBK4zgpV26XOoGh3+fT7yYN6oWBk9Pt0hw
                        m7YgIkzKw9ueLnZjspqSSG+2IwWBsyBXs1HUmxSjhjAY4DUh40pa4PBszbUVlWp+OsbxC+FOcAey
                        ++nWAnRWpfb28liDC83gBYg+EutG5PPRCRP+TI1VfIaVjcq1H7blNeL72iY8WECWXTPI/9a4hw78
                        o4iMmA4A2qWZARF+AxGQrADKt/ApIBSStOS7fN8XXCT0gqrMWaNIdAP3i9g+cRtBg/AQnprpqZE/
                        XN1Q3v+LVNGZn6aV6S6UrMvl0A6fywwB0f11LHoKwgTKH7lc5Lu8+vzuhPPEhHdLd7l65irp+ddn
                        MEx662yFKIfNtNcjtNLN28CS1e3WPeZJMaA7VXzcFtiQuQrpwXRdvTAK6fa/KETPjObkzFxwsbhn
                        HGd3wjVJiVddz6L1qErTmVKmH+vn3uPY9hkSQGBOIL8lc1dDQO04A1b4ne2j6yldhOTC4AKRzQd+
                        xkLAycauDc/b+i5YXg31nJmTPDSwcnWCeeqjYwDTuDh4F65PQrGIjemrbUR5yk39ZNTikP3bNZOD
                        iLRG6hcTFRG71QqseO3GEpnVOWYiAHUShkyBK1dINZdKz8qI2QKF4k2wo4SaAmPWVRwdTJY2a2w7
                        yCfn6LL7VhZGHm5X3+ursX0wEuFcCVjQr+/mE0e3iVNKM51NDIXrj46XspdwE+UrsWvtDW6K60m2
                        6v3f28nLn1QxltbxhR1iO/GWT+KifovUKsSuzeJpsGxTjvWrGKePDF6MnMOCaYpL2rfRFM9EH9Cm
                        ze+5pbsjInM3nwPPvI9PiDcjd+Lp8rv2cATLYvwKR1Ej19CtmGwwCbrhEzh7tFzxZEwvJvpGNG3n
                        DQr72ToIs+AWUHylEnINEnGj2VREdJKYe+J23BkGJ1gCLvN2ExMSWsPMrEqgI7kINsLTDIlYSvhK
                        S0NGJZJPakEFCwvcE6vPVnOIwHBYS5F72NDV7hp1iAHjPHLYe/HXYF7druEjPZ2dyBAq6x2HgQKO
                        xSo2hWN8nVz8SHneoTvfJJ+EVD0grsle+qdWAyOwK5CyV6a146nZqE84Tyz/cs+38S/xmMywITWS
                        1UkY/3MKfLmyp0jO4px4rVGIqtNnsRC1QHzy4pVYoqx7RSH9ZAc/9zxTIWngOcUztok4imLmJ/6S
                        zrbfpJ1/zp6dKYPc+wC01T/7ziJDnbFoWGB6KWIGfXN3ky5HaiDu0G2zWxv5D/V8UOwVyRftgP5Z
                        LzQVwrZfmFktqmDRx5fHCRtQGrB0yqE6RHDXoyGMTNJ3AtguIQstI2b2fqpsecGyUqP3b6aQOElM
                        niWXpxX5pCLd1am2Cm31OSbY+xH9PbSfoh4Q3UJSJV8keMh3qDyG9BCxdH/zCl+y8a7R/HnkbxPQ
                        K8Lbfjbet/zpZvmg3Svpmu797B327PZ9VZnubH6xLM1LJFc9H9FN7u5VW3nxjPIhOu3TPFruPfQT
                        30YYqAAAAAEABVguNTA5AAACnjCCApowggGCoAMCAQICBGPrSk8wDQYJKoZIhvcNAQELBQAwDzEN
                        MAsGA1UEAwwEdGVzdDAeFw0yMzAyMTQwODQ2MDdaFw0yNDAyMTQwODQ2MDdaMA8xDTALBgNVBAMM
                        BHRlc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDMuVOwHlsN3xvMyq18IFJ/M7vY
                        nYqYL8ehnUPkRVSTQNeIuiwHZDG9zuvV5WZ6SX/fskMj9Mfz7mUGhqi0XNsUBqAg7RIc3u7J/nGV
                        Ma6xB+bYgQ/VPO85F2n5rUkyykNLBLtwLf9mdyaziYpaKdkBrHsAGV+H8FH/5GUb+0BuVYOy5WAE
                        0YEjdQfj26o5Yo61fPMvWN6WHSDUd7Q/oxdGvn7uwOavq7WvF3DhcI3850nNNpOvbYvvGjS3Oyx9
                        d/og5ulSKbGnFdwn0UldFiI98xVmPwgyCVyy4yoWhWdv+EANwJYTNMUsYqgxpD11FDx9siApX421
                        bxSlutj8uTgVAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAEiI0D/syH3y7GW5c95pnYDPM/wbchnU
                        GEGFwitOKCe3XcwZngHbJQe7onxZr6Vo/91QbdScXYuBSskR0Jo3Xzp0uJBNkH3tDvdUHe8GIEXN
                        jvIJ9npLTPZh0gvI0gzMedo+pGpvNarqtHWoJ/eiAKVoJZ2csywQ0h8tChIuDA/ctwMAVjgzZd54
                        KiK1hPTWKtrrkbbAIRnDuL4y+G28/TzHWfOFak9zGdX02cf+IyvTaU6qZAXJMDyYApKsby6sPul/
                        HtAXvsfKnVwH8HcA4oyfjIaoTQu6+mnxHdZ/klbXYsRJXtuIKOYFIQjtb8gGGFJto61pD2zElK73
                        APkZiZL5SMcr6XQym1TwX5by/YP1aYo4lw==
                        """));
    }

    @SneakyThrows
    private static void loadKeyStore(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(inputStream, PASSWORD);
            Key key = keyStore.getKey("test", PASSWORD);
            assertThat(key).isNotNull();
        }
    }
}