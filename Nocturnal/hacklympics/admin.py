from django.contrib import admin
from hacklympics.models import *

# Register your models here.
admin.site.register(User)
admin.site.register(Course)
admin.site.register(Exam)
admin.site.register(Problem)
admin.site.register(Answer)
admin.site.register(Report)
admin.site.register(Message)
admin.site.register(Snapshot)
