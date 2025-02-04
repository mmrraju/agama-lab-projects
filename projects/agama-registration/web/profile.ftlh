[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Registration Form
	</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2/dist/tailwind.min.css" type="text/css"></head>
<body>[#ftl output_format="HTML"]





  
	
	
	
	
	
  
  <div id="content" class="md:h-screen bg-white relative flex flex-col justify-center items-center">
	  <div class="md:border md:border-gray-300 bg-white md:shadow-lg shadow-none rounded p-10 align-middle">
		<div class="flex flex-col items-center space-y-3">
		  <div class="flex">
			<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png">
		  </div>
		  <span data-i18n="profile.user_reg_form" class="text-2xl font-semi-bold leading-normal"></span>
		  <p data-i18n="profile.enter_profile_data" class="leading-normal">
		  </p>
		</div>
		<form method="post" enctype="application/x-www-form-urlencoded" class="my-8">
		  <div class="grid grid-cols-2 gap-2">
		  </div>
		  <div id="iyplr">
            <div class="relative mb-2">
                <label for="userName" data-i18n="profile.user_name" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
                    Username
                </label>
                <input type="text" id="uid" name="displayName" autofocus="" placeholder="raju" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2">
              </div>
            <div class="relative mb-2">
                <label for="displayName" data-i18n="profile.display_name" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
                    Display Name
                </label>
                <input type="text" id="displayName" name="displayName" autofocus="" placeholder="MMR Raju" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2">
            </div>
			<div class="relative mb-2">
			  <label for="givenName" data-i18n="profile.first_name" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
                First Name
			  </label>
			  <input type="text" id="givenName" name="givenName" autofocus="" placeholder="Md Mostafejur" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2">
			</div>
			<div class="relative mb-2">
			  <label for="lastname" data-i18n="profile.last_name" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
                Last Name
			  </label>
			  <input type="text" id="sn" name="sn" autofocus="" placeholder="Rahman" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2">
			</div>
            <div class="relative mb-2">
                <label for="password" data-i18n="profile.password" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
                    Password
                </label>
                <input type="password" id="password" name="password" autofocus="" required="" placeholder="Password" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2>
            </div>
			<div class=" text-sm="" flex="" justify-center="" items-center"="">
			  <button type="submit" data-callback="onSubmit" data-i18n="profile.submit" class="py-3 px-8 rounded text-white btn bg-green-500 hover:bg-green-600">
			  </button>
			</div>
		  </div>
		</form>
	  </div>
	</div>
  
  
  
  
  
  



</body>
<style>
	* {
	  box-sizing: border-box;
	}
	body {
	  margin: 0;
	}
	* {
	  margin-top: 0px;
	  margin-right: 0px;
	  margin-bottom: 0px;
	  margin-left: 0px;
	  padding-top: 0px;
	  padding-right: 0px;
	  padding-bottom: 0px;
	  padding-left: 0px;
	  font-family: 'Open Sans', sans-serif;
	}
	.input {
	  transition-duration: 0.2s;
	  transition-timing-function: ease-in-out;
	  transition-delay: 0s;
	  transition-property: border;
	  min-width: 280px;
	}
	.input:focus + .label,
	.input:active + .label,
	.input.filled + .label {
	  font-size: 0.75rem;
	  transition-duration: 0.2s;
	  transition-timing-function: ease-out;
	  transition-delay: 0s;
	  transition-property: all;
	  top: -0.9rem;
	  background-color: rgb(255, 255, 255);
	  color: rgb(26, 115, 232);
	  padding-top: 0px;
	  padding-right: 5px;
	  padding-bottom: 0px;
	  padding-left: 5px;
	  margin-top: 0px;
	  margin-right: 5px;
	  margin-bottom: 0px;
	  margin-left: 5px;
	}
	.label {
	  transition-duration: 0.2s;
	  transition-timing-function: ease-out;
	  transition-delay: 0s;
	  transition-property: all;
	  top: 0.4rem;
	  left: 0px;
	}
	.w-full.rounded.px-3.border.border-gray-300.pt-5.pb-2.focus:border-green-700.focus:ring-1.focus:ring-green-700.focus:outline-none.input.active:outline-none {
	  background-color: #eeeeee;
	}
	::-webkit-datetime-edit-year-field:not([aria-valuenow]),
	::-webkit-datetime-edit-month-field:not([aria-valuenow]),
	::-webkit-datetime-edit-day-field:not([aria-valuenow]) {
	  color: transparent;
	}
  </style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/i18next/2.0.22/i18next.min.js"></script><script src="./locale/loc-i18next.js"></script><script>
	var robot = true
	function doSubmit() {
	  if (!robot) {
		document.forms[0].requestSubmit()
	  }
	}
	//Apparently this function only gets invoked just once
	function onSubmit(token) {
	  console.log("Captcha passed")
	  robot = false
	  //Calling clip explicitly won't trigger the form submit... weird :-/
	  setTimeout(doSubmit, 100)
	}
  </script><script src="./locale/locale.js"></script>
</html>