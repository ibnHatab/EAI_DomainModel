%%% @author vlad <lib.aca55a@gmail.com>
%%% @copyright (C) 2014, vlad
%%% @doc
%%%
%%% @end
%%% Created : 16 Dec 2014 by vlad <lib.aca55a@gmail.com>

-module(request_reply).

-include_lib("eunit/include/eunit.hrl").
-include_lib("eunit_fsm/include/eunit_seq_trace.hrl").

-compile(export_all).

client_loop() ->
    receive
        {start_with,Server}  ->
            ?testTraceItit(42, ['receive', print]),
            ?testTracePrint(42,"client start"),
            Server ! {self(),{request,"REQ-1"}};
        {server,{reply,_What}} ->
            ok
    end,
    client_loop().

server_loop() ->
    receive
      {Origin, {request, What}} ->
            ?testTracePrint(42,"We are here now"),
            Origin ! {server,{reply, "RESP-1 for" ++ What}}
    end,
    server_loop().

request_reply_test_() ->
    {setup,
     fun() ->
             erlang:display(setup)
     end,
     fun(_) ->
             erlang:display(cleanup),
             catch exit(whereis(client), ok),
             catch exit(whereis(server), ok)
     end,
     ?_testTrace(
        begin
            register(client, spawn(?MODULE, client_loop, [])),
            register(server, spawn(?MODULE, server_loop, [])),
            P = whereis(client),
            P ! {start_with,whereis(server)},
            receive after 300 -> ok end
        end)
    }.

