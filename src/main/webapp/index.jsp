<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Currency Converter</title>
</head>
<body>
<h1>Currency Converter</h1>
<form method="post" action="convertCurrency">
    <label for="from">From Currency:</label>
    <input type="text" id="from" name="from" placeholder="e.g., USD" required>
    <br><br>

    <label for="to">To Currency:</label>
    <input type="text" id="to" name="to" placeholder="e.g., EUR" required>
    <br><br>

    <label for="amount">Amount:</label>
    <input type="number" id="amount" name="amount" placeholder="e.g., 100" step="0.01" required>
    <br><br>

    <button type="submit">Convert</button>
</form>
</body>
</html>
