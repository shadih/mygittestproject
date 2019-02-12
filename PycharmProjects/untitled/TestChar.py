import re

testfile = "testfile"

with open(testfile) as f:
  while True:
    c = f.read(1)
    if not c:
      print ("End of file")
      break
    print ("Read a character:", c)
    m = re.match(u"\u2018", c)
    if ( m )
      print ("MATCHED: ", c , " AS ", m)
    #= u"\u2018Hi\u2019"