package expresscheckout.controller;

import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

@Controller
public class ApplicationController   {

    final String ppUser = System.getenv("PP_USER");
    final String ppPass = System.getenv("PP_PASS");
    final String ppSign = System.getenv("PP_SIGN");



    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/cancel")
    public String cancel() {
        return "cancel";
    }

    @RequestMapping(value = "/confirm")
    public String confirm(@RequestParam("transactionKey") String transactionKey, Model model) {
        RestTemplate template = new RestTemplate();

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("transactionKey", transactionKey);
        vars.put("private_token", "SamplePrivateTestKey1");

        LinkedHashMap<String,LinkedHashMap> res = new LinkedHashMap<String,LinkedHashMap>();

        try {
            res = template.postForObject(ECURIConstants.TAXAMO+ECURIConstants.POST_CONFIRM, null, LinkedHashMap.class, vars);
            LinkedHashMap<String, Object> transaction = res.get("transaction");
            model.addAttribute("status", transaction.get("status").toString());

        } catch (HttpClientErrorException ex)   {
            if (ex.getStatusCode().value() != 404) {
                System.err.println(ex);
                //throw ex;
            }
            model.addAttribute("error", "ERROR result: " + ex.getResponseBodyAsString());
            return "error";
        } catch (Exception e)   {
            model.addAttribute("error", "Internal error");
            return "error";
        }

        model.addAttribute("transactionKey", transactionKey);

        return "confirm";
    }

    @RequestMapping(value = "/success-checkout")
    public String success(@RequestParam("token") String token, @RequestParam("taxamo_transaction_key") String transactionKey, Model model) {
        RestTemplate template = new RestTemplate();

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("transactionKey", transactionKey);
        vars.put("private_token", "SamplePrivateTestKey1");

        LinkedHashMap<String,LinkedHashMap> res = new LinkedHashMap<String,LinkedHashMap>();
        try {
            res = template.getForObject(ECURIConstants.TAXAMO+ECURIConstants.GET_TRANSACTION, LinkedHashMap.class, vars);
            LinkedHashMap<String, Object> transaction = res.get("transaction");
            model.addAttribute("total", transaction.get("total_amount").toString());

        } catch (HttpClientErrorException ex)   {
            model.addAttribute("error", "Error, status returned: " + ex.getStatusText());
            return "error";
        } catch (Exception e)   {
            System.err.println(e);
            model.addAttribute("error", "Internal error");
            return "error";
        }

        model.addAttribute("transactionKey", transactionKey);

        return "success";
    }

    @RequestMapping("/express-checkout")
    public String expressCheckout(Model model) {

        RestTemplate template = new RestTemplate();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("USER", ppUser);
        map.add("PWD", ppPass);
        map.add("SIGNATURE", ppSign);
        map.add("VERSION", "117");
        map.add("METHOD", "SetExpressCheckout");
        map.add("returnUrl", ECURIConstants.TAXAMO+ECURIConstants.SUCCESS_LINK);
        map.add("cancelUrl", ECURIConstants.TAXAMO+ECURIConstants.CANCEL_LINK);

        //shopping item(s)
        map.add("PAYMENTREQUEST_0_AMT", "20.00"); // total amount
        map.add("PAYMENTREQUEST_0_PAYMENTACTION", "Sale");
        map.add("PAYMENTREQUEST_0_CURRENCYCODE", "EUR");

        map.add("L_PAYMENTREQUEST_0_NAME0", "ProdName");
        map.add("L_PAYMENTREQUEST_0_DESC0", "ProdName desc");
        map.add("L_PAYMENTREQUEST_0_AMT0", "20.00");
        map.add("L_PAYMENTREQUEST_0_QTY0", "1");
        map.add("L_PAYMENTREQUEST_0_ITEMCATEGORY0", "Digital");

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        template.setMessageConverters(messageConverters);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders);
        ResponseEntity<String> res = template.exchange(URI.create(ECURIConstants.PAYPAL_NVP), HttpMethod.POST , request, String.class);

        Map<String, List<String>> params = parseQueryParams(res.getBody());

        String ack = params.get("ACK").get(0);
        if (!ack.equals("Success")) {
            model.addAttribute("error", params.get("L_LONGMESSAGE0").get(0));
            return "error";
        }
        else {
            String token = params.get("TOKEN").get(0);
            return "redirect:"+ECURIConstants.TAXAMO+"/checkout/index.html?"+
                    "token="+token+
                    "&public_token="+"SamplePublicTestKey1"+
                    "&billing_country_code="+"IE"+
                    "&cancel_url="+ new String(Base64.encodeBase64(new String(ECURIConstants.TAXAMO+ECURIConstants.CANCEL_LINK).getBytes()))+
                    "&return_url=" + new String(Base64.encodeBase64(new String(ECURIConstants.TAXAMO+ECURIConstants.SUCCESS_LINK).getBytes())) +
                    "#/paypal_express_checkout";
        }
    }

    private Map<String, List<String>> parseQueryParams(String qparams) {
        try {
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            for (String param : qparams.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }

                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }

            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

}