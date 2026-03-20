CREATE USER IF NOT EXISTS 'seckill_writer'@'%' IDENTIFIED BY 'writer123';
GRANT ALL PRIVILEGES ON seckill.* TO 'seckill_writer'@'%';

CREATE USER IF NOT EXISTS 'seckill_reader'@'%' IDENTIFIED BY 'reader123';
GRANT SELECT ON seckill.* TO 'seckill_reader'@'%';

CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl123';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%';

FLUSH PRIVILEGES;
