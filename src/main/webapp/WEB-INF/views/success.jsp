<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta charset="UTF-8">
<title>Successfull Checkout</title>
<link href="css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<h1>Successful purchase</h1>

<table class="shopping-cart">
		<thead>
			<tr>
				<th scope="col">Item</th>
				<th scope="col">Qty</th>
				<th scope="col">Price</th>
				<th scope="col">Currency</th>
			</tr>
		</thead>
		<tbody>
		    <tr>
		        <td>ProdName</td>
		        <td>1</td>
		        <td>${total}</td>
		        <td>EUR</td>
		    </tr>
		</tbody>
	</table>
	<br/>
	<br/>
	<hr/>
	<a href="/confirm?transactionKey=${transactionKey}&payerId=${payerId}&amount=${total}&token=${token}">
	    Confirm transaction
	</a>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>
