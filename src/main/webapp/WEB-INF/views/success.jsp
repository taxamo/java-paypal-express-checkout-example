<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta charset="UTF-8">
<title>Successfull Checkout</title>
</head>
<body>
<h1>Successfull purchase</h1>

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
	Administration:
	<a href="/confirm?transactionKey=${transactionKey}">
	    Confirm transaction
	</a>
</body>
</html>
