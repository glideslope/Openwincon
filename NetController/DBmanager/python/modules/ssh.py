
import paramiko


d_filter_keys = {
  '12':'src_ip',
  '16':'dest_ip',
  '20':'port'
}

def hex_to_dec(value):
  return int(value, 16)


def hex_to_ipaddr(value):
  ret_v = []
  for i in xrange(0, 8, 2):
    ret_v.append(int(value[i:i+2], 16))
  return '.'.join(map(str, ret_v))


class QoSHandler():
  def __init__(self, uid, upass, host, port=22):
    try:
      client = paramiko.SSHClient()
      #client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
      client.set_missing_host_key_policy(paramiko.WarningPolicy())
      client.connect(host, username=uid, password=upass)

      print "Connected to %s" % host

      self.client = client
    except paramiko.AuthenticationException:
      print "Authentication failed when connecting to %s" % host
      self.client = None
      raise
    except Exception, e:
      print "Could not SSH to %s"%(host), e

  def close(self):
    if self.client is not None:
      self.client.close()
      self.client = None
      raise

  def _send_cmd(self, cmd):
    print cmd
    return self.client.exec_command(cmd)

  def inquiry_slice(self):
    cmd = 'echo mmlab |sudo -S /home/mmlab/Openwincon/NetController/apscript/list_slice.sh'
    #cmd='echo \"mmlab\" | sudo -S ls'
    stdout = None; stderr = None;
    outlines = []
    try:
      stdin, stdout, stderr, = self._send_cmd(cmd)
      outlines = map(lambda x:x.replace('\n', ''), stdout.readlines())
    except Exception, e:
      print e
      return 
    print stdout
    return stdout.readlines()


  def inquiry_class(self, ap):
    if self.client is None:
      print 'No SSH Handler'
      raise

    stdout = None; stderr = None;
    outlines = []
    cmd = 'echo %s | sudo -S tc class show dev %s'%(ap['sudoer_passwd'], ap['target_ifname'])

    try:
      stdin, stdout, stderr, = self._send_cmd(cmd)
      outlines = map(lambda x:x.replace('\n', ''), stdout.readlines())
    except Exception, e:
      print e
      return

    classes = {}
    for outline in outlines:
      splits = outline.split()
      class_id = splits[2]
      rate = splits[7]

      classes[class_id] = rate

    return classes

  def inquiry_filter(self, ap, classid):
    if self.client is None:
      print 'No SSH Handler'
      raise

    ret = {}

    stdout = None; stderr = None;
    outlines = []
    cmd = 'echo %s | sudo -S tc filter show dev %s'%(ap['sudoer_passwd'], ap['target_ifname'])

    try:
      stdin, stdout, stderr, = self._send_cmd(cmd)
    except Exception, e:
      print e
      return 

    flowid = None; filterid = None
    matches = {}; classes = {}; filters = {}

    while True:
      line = stdout.readline()

      if line == "":
        break

      pos = line.find('flowid')

      if flowid is None and pos < 0:
        continue
      elif pos >= 0:
        if flowid is not None:
          if not classes.has_key(flowid):
            classes[flowid] = {}
          classes[flowid][filterid] = matches

        flowid = line.split()[-1]
        filterid = line.split()[9]

        matches = {}

      else: # match
        splits = line.split()
        key = d_filter_keys[splits[-1]]
        value, mask, = splits[1].split('/')
        
        if key == 'port':
          if mask[:4] == 'ffff': #sport
            key = 'src_port'
            value = str(hex_to_dec(value[:4]))
          else:
            key = 'dst_port'
            value = str(hex_to_dec(value[4:]))
        elif key[-2:] == 'ip':
          value = hex_to_ipaddr(value)
          #mask = count_successive_bits(mask)
          matches[key] = '%s/%s'%(value,mask)

        matches[key] = value

    if not classes.has_key(flowid):
      classes[flowid] = {}
    classes[flowid][filterid] = matches

    return classes


if __name__ == '__main__':
  #h = QoSHandler('mmlab', 'mmlab', '147.46.121.107')
  h = QoSHandler('mmlab', 'mmlab', '147.47.209.30')
  stdin, stdout, stderr, = h._send_cmd('echo mmlab|sudo -S ls -al')

  print ''.join(stdout.readlines())


