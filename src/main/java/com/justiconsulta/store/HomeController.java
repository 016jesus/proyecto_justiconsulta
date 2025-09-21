package com.justiconsulta.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@RestController
public class HomeController {

    @GetMapping("/api")
    public String index() {
        ApiClient api = new ApiClient(new RestTemplate(),"https://consultaprocesos.ramajudicial.gov.co:448/api/v2/");
        var response = api.get("Procesos/Consulta/NumeroRadicacion?numero={numero}&SoloActivos={SoloActivos}&pagina={pagina}",
                Map.of(
                "numero","50001333100120070007600",
                "SoloActivos",false,
                        "pagina",1)
        );

        return "<h1>Status code: " + response.getStatusCode() + "</h1>" +
                "<br>" +
                "<aside><b>Response: </b> <br>"+
                 response.getBody() +
                "</aside>";
    }


}
