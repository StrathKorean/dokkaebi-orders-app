# 도깨비 주문관리 (안드로이드 앱)

구글 Apps Script로 만든 도깨비주얼리 주문관리 웹 앱을 주소창 없이 띄우는 안드로이드 앱입니다.
자료는 구글 시트에 저장되며, 앱은 화면만 엽니다.

- 웹 앱 주소: `app/src/main/java/com/dokkaebi/orders/MainActivity.java`의 `APP_URL`
- `main` 브랜치에 올리면 GitHub Actions가 APK를 빌드해 `apk` 브랜치에 `dokkaebi-orders.apk`로 올립니다.
- 서명 키(`app/dokkaebi-debug.keystore`)가 고정되어 있어, 새로 빌드한 APK를 기존 앱 위에 그대로 설치할 수 있습니다.
