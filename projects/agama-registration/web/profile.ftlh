[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>User Registration
	</title><link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"></head>
<body>

  
	
	
	
	
  
  
	<div class="registration-container">
	  <div class="flex">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="inh1m">
	  </div>
	  <div class="header">
		<i class="fas fa-user-plus">
		</i> User Registration
	  </div>
	  <div class="content">
		<p class="text-center">To create an account, please enter your details below.
		</p>
		<form method="post">
		  <div class="form-group">
			<label class="form-label">Username: <span class="required">*</span></label>
			<input type="text" id="username" name="username" required="" placeholder="Choose a username" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">Display Name: <span class="required">*</span></label>
			<input type="text" id="displayName" name="displayName" required="" placeholder="Enter your display name" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">First Name: <span class="required">*</span></label>
			<input type="text" id="firstName" name="firstName" required="" placeholder="Enter your first name" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">Last Name: <span class="required">*</span></label>
			<input type="text" id="lastName" name="lastName" required="" placeholder="Enter your last name" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">Email: <span class="required">*</span></label>
			<input type="email" id="email" name="email" required="" placeholder="Enter your email" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">Password: <span class="required">*</span></label>
			<input type="password" id="password" name="password" required="" placeholder="Choose a strong password" class="form-control">
		  </div>
		  <div class="form-group">
			<label class="form-label">Repeat Password: <span class="required">*</span></label>
			<input type="password" id="confirmPassword" name="confirmPassword" required="" placeholder="Confirm your password" class="form-control">
			<div id="error-message" class="error-message">
			</div>
		  </div>
		  <div class="buttons">
			<button type="submit" class="btn btn-primary">Register</button>
			<button type="reset" class="btn btn-secondary">Cancel</button>
		  </div>
		</form>
	  </div>
	</div>
  
  
  
</body>
<style>
	body {
	  font-family: Arial, sans-serif;
	  background-color: #f8f9fa;
	  display: flex;
	  justify-content: center;
	  align-items: center;
	  height: 100vh;
	  margin: 0;
	}
	.registration-container {
	  width: 100%;
	  max-width: 500px;
	  background: white;
	  border-radius: 5px;
	  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
	  overflow: hidden;
	}
	.header {
	  background: #28a745;
	  padding: 15px;
	  color: white;
	  font-size: 20px;
	  font-weight: bold;
	  display: flex;
	  align-items: center;
	}
	.header i {
	  margin-right: 10px;
	}
	.content {
	  padding: 20px;
	}
	.form-group {
	  margin-bottom: 15px;
	}
	.form-label {
	  font-weight: bold;
	  display: block;
	  margin-bottom: 5px;
	}
	.form-control {
	  width: 100%;
	  padding: 10px;
	  font-size: 14px;
	  border: 1px solid #ced4da;
	  border-radius: 5px;
	}
	.required {
	  color: red;
	}
	.error-message {
	  color: red;
	  font-size: 13px;
	  margin-top: 5px;
	}
	.buttons {
	  display: flex;
	  gap: 10px;
	}
	.btn {
	  padding: 10px 15px;
	  font-size: 16px;
	  border: none;
	  border-radius: 5px;
	  cursor: pointer;
	}
	.btn-primary {
	  background-color: #007bff;
	  color: white;
	}
	.btn-primary:hover {
	  background-color: #0056b3;
	}
	.btn-secondary {
	  background-color: #6c757d;
	  color: white;
	}
	.btn-secondary:hover {
	  background-color: #5a6268;
	}
	.login-link {
	  text-align: center;
	  margin-top: 10px;
	}
	.login-link a {
	  color: #007bff;
	  text-decoration: none;
	  font-weight: bold;
	}
	.login-link a:hover {
	  text-decoration: underline;
	}
  </style>
<script>
	function validateForm() {
	  let password = document.getElementById("password").value;
	  let confirmPassword = document.getElementById("confirmPassword").value;
	  let errorMessage = document.getElementById("error-message");
	  if (password !== confirmPassword) {
		errorMessage.innerText = "Passwords do not match!";
		return false;
	  }
	  errorMessage.innerText = "";
	  return true;
	}
  </script>
</html>