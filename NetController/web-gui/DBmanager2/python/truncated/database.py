
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

   
  def get_ap_qos(self, aps=[]):
    if not self.connected:
      connect_db(self.config)

    cursor = self.cursor

    sql = """
      SELECT id, ssid, ap_ip, target_ip, target_ip_prefix_bits, target_ports,
             qos_bandwidth_uplink, qos_bandwidth_downlink
      FROM qos_rules_actions
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


  def put_or_update_ap_qos(self, data):
    if data is None:
      return

    if not self.connected:
      connect_db(self.config)

    update_values = [data['ssid'], data['ap_ip'], data['target_ip'], data['target_prefix'], data['target_ports'], data['uplink'], data['downlink']]

    cursor = self.cursor

    id = data['id']
    sql = ""

    try:
      if id != "":
        update_values_tuple = tuple(update_values+[id])
        sql = """
          UPDATE qos_rules_actions
          SET ssid = '%s', ap_ip = '%s', target_ip = '%s', target_ip_prefix_bits = %s,
              target_ports = %s, qos_bandwidth_uplink = %s, qos_bandwidth_downlink = %s
          WHERE id = %s
          """%update_values_tuple
      else:
        update_values_tuple = tuple(update_values)
        sql = """
          INSERT INTO qos_rules_actions 
            (ssid, ap_ip, target_ip, target_ip_prefix_bits, target_ports, qos_bandwidth_uplink, qos_bandwidth_downlink)
          VALUES 
            ('%s', '%s', '%s', %s, %s, %s, %s)
          """%update_values_tuple

      cursor.execute(sql)
      self.connection.commit()
    except Exception, e:
      print id
      print sql
      print data, e
      return False

    return True


  def delete_ap_qos(self, id):
    if id is None:
      return

    if not self.connected:
      connect_db(self.config)

    cursor = self.cursor

    try:
      sql = """
        DELETE FROM qos_rules_actions
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
   


