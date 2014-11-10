<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Payment confirmation - PayPal Express Checkout example</title>
<link href="css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <h1>Payment confirmed</h1>

    Transaction with id: ${transactionKey}, status: ${status}.
	<br/>
	<br/>
	<a href="/">
	    Back to Shopping Cart
	</a>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>