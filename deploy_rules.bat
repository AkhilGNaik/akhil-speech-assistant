@echo off
echo ===================================================
echo   Kannada Speech Assistant - Firestore Rules Deploy
echo ===================================================
echo.
echo This script will help you deploy the Firestore security rules.
echo First, we will sign in to your Firebase account via the browser.
echo.
pause
echo.
echo Launching Firebase Login (forcing fresh authentication)...
call firebase login --reauth
echo.
echo Deploying firestore.rules to Firebase project 'kannadaspeechassistant'...
call firebase deploy --only firestore:rules
echo.
echo Done! Please check the output above. If the deployment succeeded,
echo you can now sign up and create new accounts in your app.
echo.
pause
