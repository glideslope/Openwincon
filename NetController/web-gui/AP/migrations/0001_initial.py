# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='AP',
            fields=[
                ('id', models.AutoField(primary_key=True, serialize=False, verbose_name='ID', auto_created=True)),
                ('mac_id', models.TextField(max_length=17)),
                ('Description', models.TextField(max_length=20)),
                ('Power', models.TextField(max_length=3)),
                ('SSID', models.TextField(max_length=100)),
                ('IP', models.TextField(max_length=15)),
            ],
        ),
    ]
