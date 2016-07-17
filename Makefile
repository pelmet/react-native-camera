camera:
	 rm -rf ./node_modules/react-native-camera
	npm install react-native-camera
	react-native run-android
	adb reverse tcp:8081 tcp:8081
