%%% @author vlad <lib.aca55a@gmail.com>
%%% @copyright (C) 2014, vlad
%%% @doc
%%%
%%% @end
%%% Created : 16 Dec 2014 by vlad <lib.aca55a@gmail.com>

// Domain Model
-record(request_for_quotation,
        {rfqId::reference(), retailItems::[]}).

-record(retail_item,
        {itemId:: string(), retailPrice::float()}).

-record(request_price_quote,
        {rfqId::reference(), itemId:: string(),
         retailPrice::float(), orderTotalRetailPrice::float()}).

-record(price_quote, {rfqId::reference(), itemId::string(),
                      retailPrice::float(), discountPrice::float()}).

-record(price_quote_full_filled, {priceQuote::float()}).

-record(price_quote_timeout, {rfqId::reference()})

-record(required_price_quote_for_fullfillement,
        {rfqId::reference(), quotesRequested::integer()}).

-record(best_price_quotation, {rfqId::reference(), priceQuotes::[]}).

-record(subscri_be_to_price_quote_request,
        {quoterId:: string(), quoteProcessor::pid()}).


