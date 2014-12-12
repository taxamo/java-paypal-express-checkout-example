package com.taxamo.example.ec;
/**
 Copyright 2014 Taxamo, Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.taxamo.client.api.TaxamoApi;
import com.taxamo.client.common.ApiException;
import com.taxamo.client.model.ConfirmTransactionIn;
import com.taxamo.client.model.ConfirmTransactionOut;
import com.taxamo.client.model.GetTransactionOut;
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
import org.springframework.web.client.RestTemplate;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

@Controller
public class ApplicationController {

    final String ppUser = System.getenv("PP_USER");
    final String ppPass = System.getenv("PP_PASS");
    final String ppSign = System.getenv("PP_SIGN");

    final String privateToken = System.getenv("PRIVATE_TOKEN"); //SamplePrivateTestKey1
    final String publicToken = System.getenv("PUBLIC_TOKEN"); //SamplePublicTestKey1

    private Properties properties;

    private TaxamoApi taxamoApi;

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    /**
     * This method initializes Express Checkout token with PayPal and then redirects to Taxamo checkout form.
     *
     * Please note that only Express Checkout token is provided to Taxamo - and Taxamo will use
     * provided PayPal credentials to get order details from it and render the checkout form.
     *
     *
     * @param model
     * @return
     */
    @RequestMapping("/express-checkout")
    public String expressCheckout(Model model) {

        RestTemplate template = new RestTemplate();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("USER", ppUser);
        map.add("PWD", ppPass);
        map.add("SIGNATURE", ppSign);
        map.add("VERSION", "117");
        map.add("METHOD", "SetExpressCheckout");
        map.add("returnUrl", properties.getProperty(PropertiesConstants.STORE) + properties.getProperty(PropertiesConstants.CONFIRM_LINK));
        map.add("cancelUrl", properties.getProperty(PropertiesConstants.STORE) + properties.getProperty(PropertiesConstants.CANCEL_LINK));

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
        ResponseEntity<String> res = template.exchange(URI.create(properties.get(PropertiesConstants.PAYPAL_NVP).toString()), HttpMethod.POST, request, String.class);

        Map<String, List<String>> params = parseQueryParams(res.getBody());

        String ack = params.get("ACK").get(0);
        if (!ack.equals("Success")) {
            model.addAttribute("error", params.get("L_LONGMESSAGE0").get(0));
            return "error";
        } else {
            String token = params.get("TOKEN").get(0);
            return "redirect:" + properties.get(PropertiesConstants.TAXAMO) + "/checkout/index.html?" +
                    "token=" + token +
                    "&public_token=" + publicToken +
                    "&cancel_url=" + new String(Base64.encodeBase64((properties.getProperty(PropertiesConstants.STORE)
                    + properties.getProperty(PropertiesConstants.CANCEL_LINK)).getBytes())) +
                    "&return_url=" + new String(Base64.encodeBase64((properties.getProperty(PropertiesConstants.STORE)
                    + properties.getProperty(PropertiesConstants.CONFIRM_LINK)).getBytes())) +
                    "#/paypal_express_checkout";
        }
    }

    @RequestMapping(value = "/cancel")
    public String cancel() {
        return "cancel";
    }

    /**
     * This method is invoked after Taxamo has successfully verified tax location evidence and
     * created a transaction.
     *
     * Two things happen then:
     * - first, the express checkout token is used to capture payment in PayPal
     * - next, transaction is confirmed with Taxamo
     *
     * After that, confirmation page is displayed to the customer.
     *
     * @param payerId
     * @param token
     * @param transactionKey
     * @param model
     * @return
     */
    @RequestMapping(value = "/confirm")
    public String confirm(@RequestParam("PayerID") String payerId, @RequestParam("token") String token, @RequestParam("taxamo_transaction_key") String transactionKey, Model model) {

        RestTemplate template = new RestTemplate();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("USER", ppUser);
        map.add("PWD", ppPass);
        map.add("SIGNATURE", ppSign);
        map.add("VERSION", "117");
        map.add("METHOD", "DoExpressCheckoutPayment");
        map.add("PAYERID", payerId);
        map.add("TOKEN", token);

        GetTransactionOut transaction; //more transaction details should be verified in real-life implementation
        try {
            transaction = taxamoApi.getTransaction(transactionKey);
        } catch (ApiException e) {
            e.printStackTrace();
            model.addAttribute("error", "ERROR result: " + e.getMessage());
            return "error";
        }

        map.add("PAYMENTREQUEST_0_CURRENCYCODE", "EUR");
        map.add("PAYMENTREQUEST_0_AMT", transaction.getTransaction().getTotalAmount().toString());
        map.add("PAYMENTREQUEST_0_PAYMENTACTION", "Sale");


        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        template.setMessageConverters(messageConverters);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders);
        ResponseEntity<String> res = template.exchange(URI.create(properties.get(PropertiesConstants.PAYPAL_NVP).toString()), HttpMethod.POST, request, String.class);

        Map<String, List<String>> params = parseQueryParams(res.getBody());

        String ack = params.get("ACK").get(0);
        if (!ack.equals("Success")) {
            model.addAttribute("error", params.get("L_LONGMESSAGE0").get(0));
            return "error";
        } else {
            try {
                ConfirmTransactionOut transactionOut = taxamoApi.confirmTransaction(transactionKey, new ConfirmTransactionIn());
                model.addAttribute("status", transactionOut.getTransaction().getStatus());
            } catch (ApiException ae) {
                ae.printStackTrace();
                model.addAttribute("error", "ERROR result: " + ae.getMessage());
                return "error";
            }
            model.addAttribute("taxamo_transaction_key", transactionKey);

            return "redirect:/success-checkout";
        }
    }

    @RequestMapping(value = "/success-checkout")
    public String success(@RequestParam("taxamo_transaction_key") String transactionKey, Model model) {

        try {
            GetTransactionOut transaction = taxamoApi.getTransaction(transactionKey);
            model.addAttribute("total", transaction.getTransaction().getTotalAmount());
        } catch (ApiException ae) {
            ae.printStackTrace();
            model.addAttribute("error", "Error, status returned: " + ae.getMessage());
            return "error";
        }

        model.addAttribute("transactionKey", transactionKey);

        return "success";
    }



    @PostConstruct
    public void init() {
        properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        taxamoApi = new TaxamoApi(privateToken);
        taxamoApi.setBasePath(properties.get(PropertiesConstants.TAXAMO).toString());
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