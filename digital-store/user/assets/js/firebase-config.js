// Firebase Web Configuration
window.firebaseConfig = {
  apiKey: "AIzaSyC-X32rWmYYG_47sm20KZBl7ENdkR2uO5s",
  authDomain: "my-project-store-7047a.firebaseapp.com",
  databaseURL: "https://my-project-store-7047a-default-rtdb.firebaseio.com",
  projectId: "my-project-store-7047a",
  storageBucket: "my-project-store-7047a.appspot.com",
  messagingSenderId: "723270897314",
  appId: "1:723270897314:web:22ce4566b09412e7a291fb",
  measurementId: "G-0XL8Q6155F"
};

if (typeof firebase !== 'undefined' && firebase.initializeApp) {
  try {
    firebase.initializeApp(window.firebaseConfig);
    console.log('[Firebase JS SDK] Configured successfully for project my-project-store-7047a');
  } catch (err) {
    console.warn('[Firebase JS SDK] Init warning:', err.message);
  }
}
