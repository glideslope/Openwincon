
from operator import itemgetter

import collections
import cPickle as pickle

import MySQLdb
import json




class DBHandler():
  def __init__(self, config):
    self.connected = False
    self.config = config
    self.connect_db(config)

  def connect_db(self, config):
    if self.connected:   # For singleton
      return

    try:
      self.connection = MySQLdb.connect(**config['db_server'])
    except Exception, e:
      raise Exception(
          "Connection Error to DB server (%s)"%(config['db_server']))

    self.connected = True
    self.cursor = self.connection.cursor()


  def get_connection(self):
    if not self.connected:
      connect_db(self.config)

    return self.connection, self.cursor


  def generate_return_form(self, rs, description):
    fields = map(itemgetter(0), description)
    result = [dict(zip(fields, row)) for row in rs]

    return result


  def get_users(self, users=[]):
    if not self.connected:
      connect_db(self.config)

    cursor = self.cursor

    sql = """
      SELECT uid, uname, priority, devices
      FROM user_priority
      """

    if len(users) > 0:
      sql += "WHERE uid IN ('%s')"%("','".join(users))

    cursor.execute(sql)
    rs = cursor.fetchall()
    values = []

    for item in rs:
      value = list(item[:-1]) + [pickle.loads(item[-1])]
      values.append(value)

    return self.generate_return_form(values, cursor.description)


  def put_or_update_user(self, data):
    if data is None:
      return

    if not self.connected:
      connect_db(self.config)

    uid = data['uid']; uname = data['uname']; priority = data['priority'];
    devices = pickle.dumps(data['devices'])

    cursor = self.cursor

    try:
      sql = """
        INSERT INTO user_priority (uid, uname, priority, devices)
        VALUES ('%s', '%s', %s, '%s')
        ON DUPLICATE KEY UPDATE
          uname = '%s',
          priority = %s,
          devices = '%s'
        """%(uid, uname, priority, devices, uname, priority, devices)

      cursor.execute(sql)
      self.connection.commit()

    except Exception, e:
      print data, e
      return False

    return True


  def delete_user(self, uid):
    if uid is None:
      return

    if not self.connected:
      connect_db(self.config)

    try:
      sql = """
        DELETE FROM user_priority 
        WHERE uid = '%s'
        """%(uid)

      cursor.execute(sql)
      conn.commit()

    except Exception, e:
      print uid, e
      return False

    return True

  def delete_users(self, users=None):
    pass

   
  def get_all_aps(self, aps=[]):
    if not self.connected:
      connect_db(self.config)

    cursor = self.cursor

    sql = """
      SELECT ap_ip, ssid, target_ifname, sudoer_id, sudoer_passwd
      FROM qos_target_aps
      """

    if len(aps) > 0:
      sql += "WHERE ap_ip IN ('%s')"%("','".join(aps))

    cursor.execute(sql)
    rs = cursor.fetchall()
    values = []

    for item in rs:
      value = list(item)
      values.append(value)

    return self.generate_return_form(values, cursor.description)


  def put_or_update_aps(self, data):
    if data is None:
      return

    if not self.connected:
      connect_db(self.config)

    update_values = [data['ssid'], data['ap_ip'], data['target_ifname'], data['sudoer_id'], data['sudoer_passwd']]

    cursor = self.cursor

    id = data['id']
    sql = ""

    try:
      if id != "":
        update_values_tuple = tuple(update_values+[id])
        sql = """
          UPDATE qos_target_aps
          SET ssid = '%s', ap_ip = '%s', target_ifname = '%s',
              sudoer_id = '%s', sudoer_passwd = %s
          WHERE id = %s
          """%update_values_tuple
      else:
        update_values_tuple = tuple(update_values)
        sql = """
          INSERT INTO qos_target_aps
            (ssid, ap_ip, target_ifname, sudoer_id, sudoer_passwd)
          VALUES 
            ('%s', '%s', '%s', '%s', '%s')
          """%update_values_tuple

      cursor.execute(sql)
      self.connection.commit()
    except Exception, e:
      print data, e
      return False

    return True


  def delete_aps(self, id):
    if id is None:
      return

    if not self.connected:
      connect_db(self.config)

    cursor = self.cursor

    try:
      sql = """
        DELETE FROM qos_target_aps
        WHERE id = %s
        """%(id)

      cursor.execute(sql)
      conn.commit()

    except Exception, e:
      print sql, id, e
      return False

    return True

  def delete_all_ap_qos(self, aps=None):
    pass



if __name__ == '__main__':
  config = json.load(open('./config.json', 'r'))
  h = DBHandler(config)

  print h.get_users()
   


