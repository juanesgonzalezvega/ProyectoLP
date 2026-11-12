-- Catálogo y canje de créditos. ⚠️ costos aproximados de demostración; productos y direcciones web a verificar en los sitios oficiales.
create table if not exists public.merchants (
 id text primary key, name text not null, category text not null, website_url text,
 sort_order int not null default 0, active boolean not null default true
);
create table if not exists public.merchant_products (
 id bigint generated always as identity primary key,
 merchant_id text not null references public.merchants(id) on delete cascade,
 name text not null, description text not null default '', kind text not null,
 credit_cost int not null check (credit_cost > 0), sort_order int not null default 0,
 active boolean not null default true, unique (merchant_id,name)
);
create index if not exists merchant_products_merchant_idx on public.merchant_products(merchant_id);
create table if not exists public.redemptions (
 id bigint generated always as identity primary key,
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 product_id bigint references public.merchant_products(id) on delete set null,
 merchant_name text not null, product_name text not null,
 credits_spent int not null check (credits_spent > 0), coupon_code text not null unique,
 request_id uuid not null, created_at timestamptz not null default now(),
 expires_at timestamptz not null default now()+interval '30 days', unique(user_id,request_id)
);
create index if not exists redemptions_user_created_idx on public.redemptions(user_id,created_at desc);
alter table public.merchants enable row level security;
alter table public.merchant_products enable row level security;
alter table public.redemptions enable row level security;
revoke all on public.merchants, public.merchant_products, public.redemptions from anon, authenticated;
grant select on public.merchants, public.merchant_products, public.redemptions to authenticated;
drop policy if exists merchants_read on public.merchants;
create policy merchants_read on public.merchants for select to authenticated using (true);
drop policy if exists products_read on public.merchant_products;
create policy products_read on public.merchant_products for select to authenticated using (true);
drop policy if exists redemptions_read_own on public.redemptions;
create policy redemptions_read_own on public.redemptions for select to authenticated using (user_id=auth.uid());

insert into public.merchants(id,name,category,website_url,sort_order) values
('frisby','Frisby','Pollo','https://frisby.com.co',1),
('burger-king','Burger King','Hamburguesas',null,2),
('little-caesars','Little Caesars','Pizza',null,3),
('popsy','Popsy','Helados',null,4),
('el-corral','El Corral','Hamburguesas',null,5),
('mcdonalds','McDonald''s','Hamburguesas',null,6)
on conflict(id) do update set name=excluded.name,category=excluded.category,website_url=excluded.website_url,sort_order=excluded.sort_order,active=true;
-- Nombres y costos son demostrativos, no representan precios comerciales.
insert into public.merchant_products(merchant_id,name,description,kind,credit_cost,sort_order) values
('frisby','Combo individual de pollo apanado','Dos presas, papa y bebida.','COMBO',15,1),('frisby','Nuggets de pollo x6','Porción de seis unidades.','POLLO',9,2),('frisby','Presas de pollo x4 para compartir','Porción para compartir.','POLLO',22,3),('frisby','Papas fritas medianas','Porción mediana.','ACOMPAÑAMIENTO',5,4),('frisby','Helado de vainilla','Postre individual.','POSTRE',4,5),
('burger-king','Whopper','Hamburguesa clásica.','HAMBURGUESA',12,1),('burger-king','Combo Whopper','Incluye papas y bebida.','COMBO',17,2),('burger-king','Hamburguesa de pollo crispy','Hamburguesa de pollo.','HAMBURGUESA',11,3),('burger-king','Nuggets de pollo x10','Porción de diez unidades.','POLLO',10,4),('burger-king','Papas fritas medianas','Porción mediana.','ACOMPAÑAMIENTO',5,5),
('little-caesars','Pizza de pepperoni','Pizza de pepperoni.','PIZZA',14,1),('little-caesars','Pizza de queso','Pizza de queso.','PIZZA',12,2),('little-caesars','Pizza hawaiana','Pizza con piña.','PIZZA',14,3),('little-caesars','Crazy Bread','Pan horneado para acompañar.','ACOMPAÑAMIENTO',4,4),('little-caesars','Combo pizza y bebida','Pizza con bebida.','COMBO',17,5),
('popsy','Helado en cono','Porción individual.','POSTRE',3,1),('popsy','Copa sundae de chocolate','Helado con salsa de chocolate.','POSTRE',6,2),('popsy','Malteada','Bebida cremosa fría.','BEBIDA',7,3),('popsy','Banana split','Postre con helado y banano.','POSTRE',9,4),('popsy','Brownie con helado','Brownie acompañado de helado.','POSTRE',8,5),
('el-corral','Corral Queso','Hamburguesa con queso.','HAMBURGUESA',13,1),('el-corral','Corral Pollo','Hamburguesa de pollo.','HAMBURGUESA',13,2),('el-corral','Corral BBQ','Hamburguesa con salsa BBQ.','HAMBURGUESA',14,3),('el-corral','Papas fritas Corral','Porción de papas.','ACOMPAÑAMIENTO',5,4),('el-corral','Malteada de vainilla','Bebida cremosa fría.','BEBIDA',7,5),
('mcdonalds','Big Mac','Hamburguesa clásica.','HAMBURGUESA',12,1),('mcdonalds','Cuarto de Libra con queso','Hamburguesa con queso.','HAMBURGUESA',12,2),('mcdonalds','McNuggets x10','Porción de diez unidades.','POLLO',10,3),('mcdonalds','Papas fritas medianas','Porción mediana.','ACOMPAÑAMIENTO',5,4),('mcdonalds','McFlurry','Postre frío mezclado.','POSTRE',5,5)
on conflict(merchant_id,name) do update set description=excluded.description,kind=excluded.kind,credit_cost=excluded.credit_cost,sort_order=excluded.sort_order,active=true;

create or replace function public.redeem_product(p_product_id bigint,p_request_id uuid) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_user uuid:=auth.uid(); v_old public.redemptions%rowtype; v_product public.merchant_products%rowtype;
 v_merchant public.merchants%rowtype; v_balance int; v_code text;
begin
 if v_user is null then raise exception using message='session_expired'; end if;
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_user::text,0));
 select * into v_old from public.redemptions where user_id=v_user and request_id=p_request_id;
 if found then return jsonb_build_object('coupon_code',v_old.coupon_code,'merchant_name',v_old.merchant_name,'product_name',v_old.product_name,'credits_spent',v_old.credits_spent,'balance_after',(select coalesce(sum(l.credits),0)::int from public.credit_ledger l where l.user_id=v_user),'created_at',v_old.created_at,'expires_at',v_old.expires_at); end if;
 select * into v_product from public.merchant_products where id=p_product_id and active;
 if not found then raise exception using message='product_unavailable'; end if;
 select * into v_merchant from public.merchants where id=v_product.merchant_id and active;
 if not found then raise exception using message='product_unavailable'; end if;
 select coalesce(sum(l.credits),0)::int into v_balance from public.credit_ledger l where l.user_id=v_user;
 if v_balance<v_product.credit_cost then raise exception using message='insufficient_credits',detail=(v_product.credit_cost-v_balance)::text; end if;
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('proyectolp-coupon-codes',0));
 loop
  v_code:='PLP-'||upper(substr(md5(gen_random_uuid()::text),1,10));
  exit when not exists(select 1 from public.redemptions where coupon_code=v_code);
 end loop;
 insert into public.credit_ledger(user_id,entry_type,credits,description,dedupe_key) values(v_user,'redencion',-v_product.credit_cost,'Canje: '||v_merchant.name||' · '||v_product.name,'redeem:'||p_request_id::text);
 insert into public.redemptions(user_id,product_id,merchant_name,product_name,credits_spent,coupon_code,request_id)
 values(v_user,v_product.id,v_merchant.name,v_product.name,v_product.credit_cost,v_code,p_request_id) returning * into v_old;
 return jsonb_build_object('coupon_code',v_old.coupon_code,'merchant_name',v_old.merchant_name,'product_name',v_old.product_name,'credits_spent',v_old.credits_spent,'balance_after',v_balance-v_old.credits_spent,'created_at',v_old.created_at,'expires_at',v_old.expires_at);
end $$;
revoke all on function public.redeem_product(bigint,uuid) from public,anon;
grant execute on function public.redeem_product(bigint,uuid) to authenticated;
notify pgrst,'reload schema';
