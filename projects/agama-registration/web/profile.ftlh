[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>User Registration</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"></head>
<body><div class="registration-container shadow"><div class="header"><i class="fas fa-user-plus"></i> User Registration
        </div><p class="text-center mt-2">
            To create an account, please enter your details below. 
            <a href="#" class="fw-bold">Already have an account?</a></p><form method="post"><div class="mb-3"><label class="form-label">Username: <span class="required">*</span></label><input type="text" required="" class="form-control"></div><div class="mb-3"><label class="form-label">Display Name: <span class="required">*</span></label><input type="text" required="" class="form-control"></div><div class="mb-3"><label class="form-label">First Name: <span class="required">*</span></label><input type="text" required="" class="form-control"></div><div class="mb-3"><label class="form-label">Last Name: <span class="required">*</span></label><input type="text" required="" class="form-control"></div><div class="mb-3"><label class="form-label">Password: <span class="required">*</span></label><input type="password" id="password" required="" class="form-control"></div><div class="mb-3"><label class="form-label">Repeat password: <span class="required">*</span></label><input type="password" id="re-password" required="" class="form-control"><small id="error-message"></small></div><div class="mb-3"><label class="form-label">Email: <span class="required">*</span></label><input type="email" required="" class="form-control"></div><div class="buttons"><button type="submit" class="btn btn-primary">Register</button><button type="reset" class="btn btn-secondary">Cancel</button></div></form></div></body>
<style>
        .registration-container {
            max-width: 600px;
            margin: auto;
            border: 1px solid #28a745;
            padding: 20px;
            border-radius: 5px;
        }
        .header {
            background: #28a745;
            padding: 10px;
            color: white;
            font-size: 18px;
            font-weight: bold;
            border-radius: 5px 5px 0 0;
            display: flex;
            align-items: center;
        }
        .header i {
            margin-right: 10px;
        }
        .form-label {
            font-weight: bold;
        }
        .required {
            color: red;
        }
        .buttons {
            display: flex;
            gap: 10px;
        }
    </style>
<script>
        function validateForm() {
            let password = document.getElementById("password").value;
            let rePassword = document.getElementById("re-password").value;
            let errorMessage = document.getElementById("error-message");

            if (password !== rePassword) {
                errorMessage.innerText = "Passwords do not match!";
                errorMessage.style.color = "red";
                return false; // Prevent form submission
            }
            errorMessage.innerText = ""; // Clear error message
            return true;
        }
    </script><script src="https://kit.fontawesome.com/a076d05399.js" crossorigin="anonymous"></script>
</html>