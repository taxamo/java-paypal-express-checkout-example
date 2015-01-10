## Taxamo Java PayPal ExpressCheckout example

This guide shows how to integrate PayPal's ExpressCheckout with Taxamo RESTful API and also serves as an example for using Taxamo Java API client library.

*Complete source codes for this example are located in [https://github.com/taxamo/java-paypal-express-checkout-example](https://github.com/taxamo/java-paypal-express-checkout-example) GitHub repository.*

First step to use PayPal ExpressCheckout is to open developer account at http://developer.paypal.com. Next, you need to get your Classic 
TEST API credentials (signature) (get it from [sandbox/accounts](https://developer.paypal.com/webapps/developer/applications/accounts) details page, 
see picture below, or check PayPal [documentation](https://developer.paypal.com/docs/classic/api/apiCredentials/)).

![PayPal API credentials](https://dl.dropboxusercontent.com/u/39202878/pp_credentials.png)

As already mentioned, you need to provide those to 
[PayPal PSP configuration screen](https://beta.taxamo.com/merchant/app.html!/account/payment-gateways/paypal) in Taxamo dashboard.

There are two options to integrate ExpressCheckout in your app:

 - shopping cart experience
 - payment option

In this example, we've used first option, adding PayPal's ExpressCheckout button on simple Shopping cart page.

```html
<a href="/express-checkout">
    <img src="https://www.paypal.com/en_US/i/btn/btn_xpressCheckout.gif" align="left" style="margin-right:7px;">
</a>
```

By clicking on this button your app should create Express Checkout token with PayPal for current Shopping cart state (by calling PayPal **SetExpressCheckout** API), 
and then redirect customer's browser to Taxamo. This is done with following route controller:

```java
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
```

After the customer finishes with PayPal payment, Taxamo will redirect user to page you specified (or in case of cancellation, to specific page for that case).

At that point, your store should finalize payment, by calling PayPal **DoExpressCheckoutPayment** API, and confirm transaction with Taxamo using RESTful API.

Calling Taxamo RESTful API is as simple as calling a method on `TaxamoApi` instance (upon whose initialization private token should be supplied).

Code for confirmation of transaction is stated below:

```java
try {
    taxamoApi.confirmTransaction(transactionKey, new ConfirmTransactionIn());
}
catch (ApiException ae) {
    ...
}
```

TaxamoApi is part of [taxamo-java](https://github.com/taxamo/taxamo-java) library, which needs to be added as dependency to your project.

In this example we have used SpringMVC for simple request/response handling and Bootstrap 3 for simple UI templating. Both are optional, you can use ones by your choice.

To run the app in test/sandbox mode, run following command (providing correct env_vars - you should use your account's Taxamo tokens and PayPal credentials):

```
PRIVATE_TOKEN='SamplePrivateTestKey1' \
PUBLIC_TOKEN='SamplePublicTestKey1' \
PP_USER='tomek-facilitator_api1.lipski.net.pl' \
PP_PASS='1381909296' \
PP_SIGN='AFcWxV21C7fd0v3bYYYRCpSSRl31AmaVKlMrHDz6A-O8RusKMWO9DnWn' \
mvn -Djetty.port=3009 jetty:run
```

Then open http://localhost:3009 in your browser.

Whole process of ExpressCheckout is depicted with following diagram:

![Taxamo PayPal EC diagram](https://cloud.githubusercontent.com/assets/876542/5693514/7532e924-9920-11e4-91fb-3638042b85c0.png)
