%%% @author vlad <lib.aca55a@gmail.com>
%%% @copyright (C) 2014, vlad
%%% @doc
%%%
%%% @end
%%% Created : 16 Dec 2014 by vlad <lib.aca55a@gmail.com>

-module(return_address).

-include_lib("eunit/include/eunit.hrl").
-include_lib("eunit_fsm/include/eunit_seq_trace.hrl").

-compile(export_all).

-record(request_complex,{what}).
-record(reply_complex,{what}).

worker() ->
    receive
        {From,#request_complex{} = Req} ->
            From ! #reply_complex{ what = "RESP-1 for " ++ Req#request_complex.what }
    end,
    worker().

server() ->
    receive
	#request_complex{} = Req ->
            ?testTraceItit(42, ['receive', print]),
            ?testTracePrint(42,"server start"),
            Worker = whereis(worker),
            Worker ! {self(),Req};
        _ -> discard
    end,
    server().

return_address_test_() ->
    {setup,
     fun() ->
             erlang:display(setup)
     end,
     fun(_) ->
             erlang:display(cleanup),
             catch exit(whereis(worker), ok),
             catch exit(whereis(server), ok)
     end,
     ?_testTrace(
        begin
            register(worker, spawn(?MODULE, worker, [])),
            register(server, spawn(?MODULE, server, [])),
            P = whereis(server),
            P ! #request_complex{what = "JOB-1"},
            receive after 300 -> ok end
        end)
    }.



