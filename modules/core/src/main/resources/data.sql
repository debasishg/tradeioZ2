insert into accounts
values(
  'ibm-123',
  'IBM',
  'Trading',
  '2019-06-22 19:10:25',
  null,
  'USD',
  'USD',
  null
);

insert into accounts
values(
  'ibm-124',
  'IBM',
  'Trading',
  '2019-08-22 19:10:25',
  null,
  'USD',
  'USD',
  null
);

insert into accounts
values(
  'nri-654',
  'Nomura',
  'Trading',
  '2019-08-25 19:10:25',
  null,
  'USD',
  'USD',
  null
);

insert into instruments
values (
  'US0378331005',
  'apple',
  'equity',
  '2019-08-25 19:10:25',
  null,
  100,
  1200.50,
  null,
  null
);

insert into instruments
values (
  'GB0002634946',
  'bae systems',
  'equity',
  '2018-08-25 19:10:25',
  null,
  100,
  200.50,
  null,
  null
);

insert into taxFees (taxFeeId, description)
values 
  ('TradeTax', 'Trade Tax'),
  ('Commission', 'Commission'),
  ('VAT', 'VAT'),
  ('Surcharge', 'Surcharge');

insert into users 
values (uuid_generate_v4(), 'debasish', 'toughgraff');