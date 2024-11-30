from django.urls import path
from home.views import uploadApp


urlpatterns = [
    # Receive App
    path('receiveApp/', uploadApp.as_view(), name='upload_app'),
]

